import html
import os

from django.http import HttpResponse


EXPECTED_VALUE = "loaded-from-mise-for-issue-514"


def _env_status() -> dict[str, str | bool | None]:
    value = os.environ.get("ISSUE514_DJANGO_MISE_ENV")
    debug_marker = os.environ.get("ISSUE514_DJANGO_DEBUG_MARKER")

    return {
        "loaded": value == EXPECTED_VALUE,
        "ISSUE514_DJANGO_MISE_ENV": value,
        "ISSUE514_DJANGO_DEBUG_MARKER": debug_marker,
    }


def index(request):
    status = _env_status()
    rows = "\n".join(
        "<tr>"
        f"<th>{html.escape(str(key))}</th>"
        f"<td>{html.escape(str(value))}</td>"
        "</tr>"
        for key, value in status.items()
    )
    page_status = "loaded" if status["loaded"] else "missing"

    return HttpResponse(
        f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Issue 514 Django env check</title>
  <style>
    body {{
      color: #202124;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      margin: 40px;
    }}
    h1 {{
      font-size: 28px;
      font-weight: 650;
      margin: 0 0 20px;
    }}
    table {{
      border-collapse: collapse;
      min-width: 520px;
    }}
    th,
    td {{
      border: 1px solid #d9dee7;
      padding: 10px 12px;
      text-align: left;
    }}
    th {{
      background: #f5f7fb;
      font-weight: 600;
    }}
    .status {{
      color: {"#137333" if status["loaded"] else "#b3261e"};
      font-weight: 650;
    }}
  </style>
</head>
<body>
  <h1>Issue 514 Django env check: <span class="status">{page_status}</span></h1>
  <table>{rows}</table>
</body>
</html>"""
    )
