from selenium import webdriver
from selenium.webdriver.chrome.options import Options


def create_browser_driver(config):
    opts = Options()
    if config["browser"].get("headless"):
        opts.add_argument("--headless=new")
    opts.add_argument("--no-sandbox")
    opts.add_argument("--disable-dev-shm-usage")
    opts.add_argument("--ignore-certificate-errors")
    opts.add_argument("--allow-running-insecure-content")
    opts.add_argument("--autoplay-policy=no-user-gesture-required")
    # Required for browser-side publishing (fake camera/mic instead of real device)
    opts.add_argument("--use-fake-device-for-media-stream")
    opts.add_argument("--use-fake-ui-for-media-stream")
    # Allow getUserMedia on the standalone HTTP origin
    standalone_ip = config["red5"].get("standalone_ip", "")
    if standalone_ip:
        opts.add_argument(
            f"--unsafely-treat-insecure-origin-as-secure=http://{standalone_ip}:5080"
        )
    return webdriver.Chrome(options=opts)
