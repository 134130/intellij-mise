import os

def main():
    env_vars = os.environ
    if "MISE_PYTHON_TEST" in env_vars:
        print("MISE_PYTHON_TEST is set")
    else:
        print("MISE_PYTHON_TEST is NOT set")

if __name__ == "__main__":
    main()
