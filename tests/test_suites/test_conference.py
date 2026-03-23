import logging

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from pages.main_page import MainPage
from pages.conference_page import ConferencePage

log = logging.getLogger(__name__)


def _browser_join(driver, url, name, timeout=15):
    """Navigate to the conference URL, fill name, submit, wait for leave button — confirms joined."""
    log.info("[browser] Navigating to conference: %s", url)
    wait = WebDriverWait(driver, timeout)
    driver.get(url)
    wait.until(EC.presence_of_element_located((By.ID, "participant_name")))
    driver.find_element(By.ID, "participant_name").send_keys(name)
    driver.find_element(By.ID, "room_join_button").click()
    log.info("[browser] Joining as '%s', waiting for leave button to confirm join...", name)
    wait.until(EC.presence_of_element_located((By.ID, "leave-room-button")))
    log.info("[browser] '%s' joined successfully", name)


def test_conference(configured_app, browser_driver, config):
    """
    Android joins conference as publisher.
    Two browser tabs join the same room.
    Verifies:
      - Android participantCountText == "Total Participants: 3"
      - Android grid has 2 participant tiles
      - Browser tab 1 has a visible red5pro-subscriber video element for Android's stream
    """
    driver = configured_app
    r5 = config["red5"]
    sm_url = r5["sm_url"].rstrip("/")
    room = r5["conference_room"]
    conference_url = f"{sm_url}/meetings/{room}"

    log.info("Conference URL: %s", conference_url)

    # --- Android: join as publisher ---
    log.info("[android] Navigating to Conference activity")
    main = MainPage(driver)
    main.tap_activity("Conference")

    conf_page = ConferencePage(driver, timeout=30)
    conf_page.enter_room_id(room)
    conf_page.enter_user_name("android-test")
    log.info("[android] Joining room '%s' as publisher", room)
    conf_page.tap_join()
    conf_page.wait_for_connected()
    log.info("[android] Connected to conference")

    # --- Browser tab 1: join ---
    log.info("[browser] Tab 1 joining...")
    _browser_join(browser_driver, conference_url, "webtest1")

    # --- Browser tab 2: open new tab, join ---
    log.info("[browser] Opening tab 2...")
    browser_driver.execute_script("window.open('', '_blank')")
    browser_driver.switch_to.window(browser_driver.window_handles[1])
    log.info("[browser] Tab 2 joining...")
    _browser_join(browser_driver, conference_url, "webtest2")

    # --- Android: verify participant count ---
    log.info("[android] Waiting for participant count to reach 3...")
    conf_page.wait_for_participant_count(3)
    count_text = conf_page.get_participant_count_text()
    log.info("[android] Participant count text: '%s'", count_text)
    assert count_text == "Total Participants: 3"

    # --- Android: verify both web participants are visible by name ---
    log.info("[android] Waiting for webtest1 tile to appear...")
    conf_page.wait_for_participant_name_visible("webtest1")
    log.info("[android] webtest1 tile visible")

    log.info("[android] Waiting for webtest2 tile to appear...")
    conf_page.wait_for_participant_name_visible("webtest2")
    log.info("[android] webtest2 tile visible")

    # --- Browser tab 1: verify Android's video element is visible ---
    log.info("[browser] Switching to tab 1 to verify Android video element")
    browser_driver.switch_to.window(browser_driver.window_handles[0])
    browser_wait = WebDriverWait(browser_driver, 15)
    log.info("[browser] Waiting for red5pro-subscriber video element...")
    browser_wait.until(
        EC.presence_of_element_located(
            (By.CSS_SELECTOR, 'video[id^="red5pro-subscriber-"]')
        )
    )
    video = browser_driver.find_element(By.CSS_SELECTOR, 'video[id^="red5pro-subscriber-"]')
    video_id = video.get_attribute("id")
    video_style = video.get_attribute("style")
    log.info("[browser] Found video element: id=%s style=%s", video_id, video_style)
    assert "visibility: visible" in video_style, (
        f"Android subscriber video not visible. id={video_id} style={video_style}"
    )
    log.info("[browser] Android video is visible — stream confirmed")

    # --- Close webtest2 tab, verify participant drops on Android ---
    log.info("[browser] Closing webtest2 tab to simulate participant leave")
    browser_driver.switch_to.window(browser_driver.window_handles[1])
    browser_driver.close()
    browser_driver.switch_to.window(browser_driver.window_handles[0])

    log.info("[android] Waiting for participant count to drop to 2...")
    conf_page.wait_for_participant_count(2)
    count_text = conf_page.get_participant_count_text()
    log.info("[android] Participant count after webtest2 left: '%s'", count_text)
    assert count_text == "Total Participants: 2"

    log.info("[android] Verifying webtest2 tile is gone...")
    conf_page.wait_for_participant_name_gone("webtest2")
    log.info("[android] webtest2 tile removed")

    # --- Teardown ---
    log.info("[android] Leaving conference")
    conf_page.leave()
    driver.back()

    log.info("[browser] Resetting tab 1 to blank")
    browser_driver.switch_to.window(browser_driver.window_handles[0])
    browser_driver.get("about:blank")
    log.info("Conference test complete")
