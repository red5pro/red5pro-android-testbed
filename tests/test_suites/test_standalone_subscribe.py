from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from pages.main_page import MainPage
from pages.subscribe_page import SubscribePage


def test_standalone_subscribe(configured_app, browser_driver, config, unique_stream_name):
    """
    Browser publishes to standalone Red5 server (auto-publishes on load, fake camera).
    Android app subscribes via the Standalone Subscribe activity.
    Verifies:
      - browser #status-field contains "Started publishing session."
      - Android status_indicator_text becomes "Live"
      - Android btn_subscribe reads "STOP SUBSCRIBE"
    """
    driver = configured_app
    r5 = config["red5"]
    ip = r5["standalone_ip"]
    stream_name = unique_stream_name
    app_name = r5.get("app_name", "live")

    publish_url = (
        f"http://{ip}:5080/webrtcexamples/test/publish/"
        f"?app={app_name}&streamName={stream_name}"
    )

    # --- Browser: start publishing (auto-publishes on load) ---
    browser_driver.get(publish_url)

    browser_wait = WebDriverWait(browser_driver, 15)
    browser_wait.until(
        lambda d: "Started publishing session"
        in d.find_element(By.ID, "status-field").text
    )

    assert "Started publishing session" in browser_driver.find_element(
        By.ID, "status-field"
    ).text

    # --- Android: subscribe to the stream ---
    main = MainPage(driver)
    main.tap_activity("Subscribe Standalone")

    subscribe_page = SubscribePage(driver, timeout=30)

    assert subscribe_page.get_status() == "Disconnected"
    assert subscribe_page.get_subscribe_button_text() == "START SUBSCRIBE"

    subscribe_page.tap_subscribe()
    subscribe_page.wait_for_live()

    assert subscribe_page.get_status() == "Live"
    assert subscribe_page.get_subscribe_button_text() == "STOP SUBSCRIBE"

    # --- Teardown ---
    subscribe_page.stop_subscribe()
    subscribe_page.wait_for_disconnected()
    driver.back()
