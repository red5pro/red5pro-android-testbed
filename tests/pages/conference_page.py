from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

PACKAGE = "net.red5.testbed"


class ConferencePage:
    def __init__(self, driver, timeout=30):
        self.driver = driver
        self.wait = WebDriverWait(driver, timeout)

    def _id(self, name):
        return f"{PACKAGE}:id/{name}"

    def enter_room_id(self, room_id):
        el = self.wait.until(EC.presence_of_element_located((AppiumBy.ID, self._id("roomIdInput"))))
        el.clear()
        el.send_keys(room_id)

    def enter_user_name(self, name):
        el = self.wait.until(EC.presence_of_element_located((AppiumBy.ID, self._id("userNameInput"))))
        el.clear()
        el.send_keys(name)

    def tap_join(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("joinButton")))
        ).click()

    def wait_for_connected(self):
        WebDriverWait(self.driver, 20).until(
            lambda d: "Connected" in d.find_element(
                AppiumBy.ID, self._id("statusText")
            ).text
        )

    def wait_for_participant_count(self, count, timeout=20):
        WebDriverWait(self.driver, timeout).until(
            lambda d: d.find_element(
                AppiumBy.ID, self._id("participantCountText")
            ).text == f"Total Participants: {count}"
        )

    def get_participant_count_text(self):
        return self.driver.find_element(AppiumBy.ID, self._id("participantCountText")).text

    def wait_for_participant_name_visible(self, name, timeout=20):
        WebDriverWait(self.driver, timeout).until(
            EC.presence_of_element_located(
                (AppiumBy.ANDROID_UIAUTOMATOR, f'new UiSelector().textContains("{name}")')
            )
        )

    def wait_for_participant_name_gone(self, name, timeout=20):
        WebDriverWait(self.driver, timeout).until(
            lambda d: len(
                d.find_elements(
                    AppiumBy.ANDROID_UIAUTOMATOR, f'new UiSelector().textContains("{name}")'
                )
            ) == 0
        )

    def leave(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("leaveButton")))
        ).click()
