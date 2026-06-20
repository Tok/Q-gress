// Run headless Chrome with --no-sandbox so Karma tests work inside
// containers / CI where the Chrome sandbox is unavailable.
config.set({
    browsers: ["ChromeHeadlessNoSandbox"],
    customLaunchers: {
        ChromeHeadlessNoSandbox: {
            base: "ChromeHeadless",
            flags: ["--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage"]
        }
    }
});
