from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from pages.main_page import MainPage
from pages.publish_page import PublishPage


def test_standalone_publish(configured_app, browser_driver, config, unique_stream_name):
    """
    Android publishes to standalone Red5 server.
    Browser navigates to the standalone subscriber test page (auto-subscribes on load).
    Verifies:
      - browser #status-field contains "Started subscribing session."
      - inbound-rtp bytesReceived > 0 via WebRTC stats
    """
    driver = configured_app
    r5 = config["red5"]
    ip = r5["standalone_ip"]
    stream_name = unique_stream_name
    app_name = r5.get("app_name", "live")

    subscribe_url = (
        f"http://{ip}:5080/webrtcexamples/test/subscribe/"
        f"?app={app_name}&streamName={stream_name}"
    )

    # --- Android: navigate to Standalone Publish ---
    main = MainPage(driver)
    main.tap_activity("Publish Standalone")

    publish_page = PublishPage(driver, timeout=30)

    publish_page.wait_for_preview()

    assert publish_page.get_status() == "Disconnected"
    assert publish_page.get_publish_button_text() == "START PUBLISH"

    publish_page.tap_publish()
    publish_page.wait_for_live()

    assert publish_page.get_status() == "Live"
    assert publish_page.get_publish_button_text() == "STOP PUBLISH"

    # --- Browser: capture RTCPeerConnection before page loads ---
    browser_driver.execute_cdp_cmd(
        "Page.addScriptToEvaluateOnNewDocument",
        {
            "source": """
                window._rtcPeers = [];
                const _Orig = window.RTCPeerConnection;
                window.RTCPeerConnection = function(...args) {
                    const pc = new _Orig(...args);
                    window._rtcPeers.push(pc);
                    return pc;
                };
                window.RTCPeerConnection.prototype = _Orig.prototype;
            """
        },
    )

    browser_driver.get(subscribe_url)

    browser_wait = WebDriverWait(browser_driver, 15)
    browser_wait.until(
        lambda d: "Started subscribing session"
        in d.find_element(By.ID, "status-field").text
    )

    assert "Started subscribing session" in browser_driver.find_element(
        By.ID, "status-field"
    ).text

    stats = browser_driver.execute_async_script(
        """
        const done = arguments[0];
        const pc = window._rtcPeers && window._rtcPeers[0];
        if (!pc) return done([]);
        pc.getStats().then(report => {
            const entries = [];
            report.forEach(s => entries.push(s));
            done(entries);
        });
        """
    )

    inbound_rtp = [s for s in stats if s.get("type") == "inbound-rtp"]
    assert inbound_rtp, "No inbound-rtp stats found — peer connection not established"
    assert any(s.get("bytesReceived", 0) > 0 for s in inbound_rtp), (
        f"bytesReceived is 0 on all inbound-rtp entries: {inbound_rtp}"
    )

    # --- Teardown ---
    publish_page.stop_publish()
    publish_page.wait_for_disconnected()
    driver.back()
