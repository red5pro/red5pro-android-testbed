import os
import time
import yaml
import pytest

from helpers.android_driver import create_android_driver
from helpers.browser_driver import create_browser_driver
from pages.settings_page import SettingsPage
from pages.main_page import MainPage


def _load_config():
    config_path = os.environ.get(
        "TEST_CONFIG",
        os.path.join(os.path.dirname(__file__), "config", "local.yaml"),
    )
    with open(config_path) as f:
        cfg = yaml.safe_load(f)

    r5 = cfg["red5"]
    # Environment variables override local.yaml values — used in CI
    r5["license_key"] = os.environ.get("LICENSE_KEY", r5.get("license_key", ""))
    r5["sm_url"] = os.environ.get("SM_URL", r5.get("sm_url", ""))
    r5["standalone_ip"] = os.environ.get("STANDALONE_IP", r5.get("standalone_ip", ""))
    r5["conference_room"] = os.environ.get("CONFERENCE_ROOM", r5.get("conference_room", "testroom"))
    r5["stream_name"] = os.environ.get("STREAM_NAME", r5.get("stream_name", "testStream"))
    r5["username"] = os.environ.get("R5_USERNAME", r5.get("username", ""))
    r5["password"] = os.environ.get("R5_PASSWORD", r5.get("password", ""))
    return cfg


@pytest.fixture(scope="session")
def config():
    return _load_config()


@pytest.fixture(scope="session")
def android_driver(config):
    driver = create_android_driver(config)
    yield driver
    driver.quit()


@pytest.fixture(scope="session")
def browser_driver(config):
    driver = create_browser_driver(config)
    yield driver
    driver.quit()


@pytest.fixture(scope="session")
def configured_app(android_driver, config):
    """Open Settings, fill all required fields, save — runs once per session."""
    main = MainPage(android_driver)
    main.tap_activity("Settings")

    settings = SettingsPage(android_driver)
    r5 = config["red5"]

    settings.set_license_key(r5["license_key"])
    settings.set_stream_manager_host(r5["sm_url"])
    if r5.get("standalone_ip"):
        settings.set_standalone_server_ip(r5["standalone_ip"])
    settings.set_app_name(r5.get("app_name", "live"))

    if r5.get("username"):
        settings.set_username(r5["username"])
    if r5.get("password"):
        settings.set_password(r5["password"])

    settings.save()  # presses back, returns to MainActivity

    yield android_driver


@pytest.fixture(scope="function")
def unique_stream_name(configured_app, config):
    """Generate a unique stream name and write it to Settings before each test."""
    base = config["red5"]["stream_name"]
    name = f"{base}{int(time.time())}"

    main = MainPage(configured_app)
    main.tap_activity("Settings")

    settings = SettingsPage(configured_app)
    settings.set_stream_name(name)
    settings.save()  # presses back, returns to MainActivity

    return name
