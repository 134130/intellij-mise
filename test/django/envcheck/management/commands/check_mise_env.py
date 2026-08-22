import os

from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    help = "Check whether mise-provided environment variables are present."

    def handle(self, *args, **options):
        value = os.environ.get("ISSUE514_DJANGO_MISE_ENV")
        debug_marker = os.environ.get("ISSUE514_DJANGO_DEBUG_MARKER")

        self.stdout.write(f"ISSUE514_DJANGO_MISE_ENV={value!r}")
        self.stdout.write(f"ISSUE514_DJANGO_DEBUG_MARKER={debug_marker!r}")

        if value != "loaded-from-mise-for-issue-514":
            raise CommandError("mise environment was not loaded")

        self.stdout.write(self.style.SUCCESS("mise environment is loaded"))
