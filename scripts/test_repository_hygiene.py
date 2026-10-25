"""Self-contained regression tests for the repository hygiene gate."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from check_repository_hygiene import hygiene_errors


class RepositoryHygieneTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "README.md").write_text("# Test\n", encoding="utf-8")
        for directory in ("app", "docs", "scripts", ".github"):
            (self.root / directory).mkdir()

    def test_clean_root_allows_archived_rewrites(self) -> None:
        archived = self.root / "scripts/archive/legacy-code-patches"
        archived.mkdir(parents=True)
        (archived / "patch_main.py").write_text("old", encoding="utf-8")
        self.assertEqual([], hygiene_errors(self.root))

    def test_rejects_root_level_mutating_scripts(self) -> None:
        (self.root / "patch_main.py").write_text("old", encoding="utf-8")
        (self.root / "rewrite_services.py").write_text("old", encoding="utf-8")
        errors = hygiene_errors(self.root)
        self.assertEqual(1, len(errors))
        self.assertIn("patch_main.py", errors[0])
        self.assertIn("rewrite_services.py", errors[0])

    def test_missing_readme_is_not_silent(self) -> None:
        (self.root / "README.md").unlink()
        self.assertTrue(any("README.md" in error for error in hygiene_errors(self.root)))


if __name__ == "__main__":
    unittest.main()
