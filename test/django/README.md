# Issue 514 Django Reproduction

Minimal Django project for reproducing issue #514: PyCharm loads mise env vars for a normal Python/Django run configuration, but not for debug.

## Setup

```sh
cd test/django
uv sync
```

## Command Line Check

```sh
mise run check-env
mise run server
```

Open http://127.0.0.1:8000/ and confirm `ISSUE514_DJANGO_MISE_ENV` is `loaded-from-mise-for-issue-514`.

## PyCharm Reproduction

1. Open `test/django` as a PyCharm project.
2. Create a Python run configuration for `manage.py`.
3. Use parameters `runserver 127.0.0.1:8000`.
4. Enable `Use environment variables from mise`.
5. Select `Use project settings`.
6. Run the configuration and confirm the page shows the mise env value.
7. Debug the same configuration and confirm whether the value is still present.

Expected result: run and debug both include `ISSUE514_DJANGO_MISE_ENV`.
