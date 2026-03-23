from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

PACKAGE = "net.red5.testbed"


class PublishPage:
    def __init__(self, driver, timeout=30):
        self.driver = driver
        self.wait = WebDriverWait(driver, timeout)

    def _id(self, name):
        return f"{PACKAGE}:id/{name}"

    def wait_for_preview(self):
        """Wait until camera preview starts — licence validated and camera open.
        Proxy: btn_switch_camera becomes enabled inside onPreviewStarted()."""
        self.wait.until(
            lambda d: d.find_element(AppiumBy.ID, self._id("btn_switch_camera")).is_enabled()
        )

    def tap_publish(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("btn_publish")))
        ).click()

    def wait_for_live(self):
        WebDriverWait(self.driver, 20).until(
            lambda d: d.find_element(
                AppiumBy.ID, self._id("status_indicator_text")
            ).text == "Live"
        )

    def stop_publish(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("btn_publish")))
        ).click()

    def wait_for_disconnected(self):
        WebDriverWait(self.driver, 15).until(
            lambda d: d.find_element(
                AppiumBy.ID, self._id("status_indicator_text")
            ).text == "Disconnected"
        )

    def get_status(self):
        return self.driver.find_element(AppiumBy.ID, self._id("status_indicator_text")).text

    def get_publish_button_text(self):
        return self.driver.find_element(AppiumBy.ID, self._id("btn_publish")).text
