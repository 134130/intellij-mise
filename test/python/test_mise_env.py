import os

def test_mise_env():
    env_vars = os.environ
    if "MISE_PYTHON_TEST" in env_vars:
        print("MISE_PYTHON_TEST is set")
    else:
        print("MISE_PYTHON_TEST is NOT set")
    assert "MISE_PYTHON_TEST" in os.environ

if __name__ == '__main__':
    test_mise_env()