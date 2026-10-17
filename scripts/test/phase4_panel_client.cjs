"use strict";

// Test-only client: Python owns Chrome and every OS process in this scenario.
const fs = require("node:fs");
const path = require("node:path");
const { randomUUID } = require("node:crypto");
const { setTimeout: delay } = require("node:timers/promises");

function should(condition, message) {
  if (!condition) throw new Error(`Then ${message}`);
}

function argumentsOf(argv) {
  const result = {};
  const names = ["cdp-url", "base-url", "artifact", "result", "playwright-package-root"];
  for (let index = 0; index < argv.length; index += 2) {
    const name = argv[index]?.slice(2);
    should(argv[index]?.startsWith("--") && names.includes(name) && argv[index + 1] && !result[name],
      "explicit unique client arguments must be complete");
    result[name] = argv[index + 1];
  }
  should(names.every((name) => result[name]), "all explicit client arguments must be present");
  return result;
}

function loopbackOrigin(value) {
  const url = new URL(value);
  should(url.protocol === "http:" && url.hostname === "127.0.0.1" &&
    /^\d+$/.test(url.port) && url.pathname === "/" && !url.username && !url.password &&
    !url.search && !url.hash, "the exact endpoint must be a numeric loopback HTTP origin with a port");
  return url.origin;
}

async function main(argv) {
  let receipt, browser, context, page, watchdog;
  const result = { success: false, error: null, actions: [], screenshots: [],
    pageErrors: [], consoleErrors: [], optionalApplicationDiagnostics: [],
    blockedRequests: [], cleanupErrors: [], terminal: false };
  function save() {
    const temporary = `${receipt}.${randomUUID()}.tmp`;
    fs.writeFileSync(temporary, `${JSON.stringify(result, null, 2)}\n`, { mode: 0o600 });
    fs.renameSync(temporary, receipt);
  }
  function clause(kind, message) {
    console.log(`${kind} ${message}`);
    result.actions.push({ clause: kind, message, at: new Date().toISOString() });
    save();
  }
  try {
    const selected = argv.indexOf("--result");
    const selectedPath = argv[selected + 1];
    should(selected >= 0 && selectedPath && path.isAbsolute(selectedPath), "an absolute local result path is required");
    const resultPath = path.resolve(selectedPath);
    const output = path.resolve(__dirname, "../../target/phase-4-packaged-operations");
    const relative = path.relative(output, resultPath);
    should(relative.startsWith("case-") && !relative.split(path.sep).includes("..") &&
      path.basename(resultPath) === "panel-result.json", "receipts must remain in the private target case directory");
    receipt = resultPath;
    save(); // Initial receipt precedes package loading, CDP connection and browser actions.
    const args = argumentsOf(argv);
    const cdp = loopbackOrigin(args["cdp-url"]);
    const base = loopbackOrigin(args["base-url"]);
    should(cdp !== base && args.artifact === "textus-supervisor", "CDP and target must match the selected private case");
    const packageRoot = args["playwright-package-root"];
    should(path.isAbsolute(packageRoot) && fs.statSync(packageRoot).isDirectory(), "Playwright root must be absolute and existing");
    const metadata = JSON.parse(fs.readFileSync(path.join(packageRoot, "package.json"), "utf8"));
    should(metadata.name === "playwright", "the explicit package must be actual Playwright");
    const { chromium } = require(packageRoot);
    clause("Given", "the actual packaged panel and Python-acquired private Chrome endpoint");
    browser = await chromium.connectOverCDP(cdp, { timeout: 30000 });
    context = await browser.newContext({ viewport: { width: 1280, height: 900 }, acceptDownloads: false });
    await context.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.hostname === "127.0.0.1" && ["http:", "https:"].includes(url.protocol)) await route.continue();
      else { result.blockedRequests.push(url.href); await route.abort("blockedbyclient"); }
    });
    page = await context.newPage();
    page.setDefaultTimeout(20000);
    page.setDefaultNavigationTimeout(25000);
    page.on("pageerror", (error) => result.pageErrors.push(error.stack || error.message));
    page.on("console", (message) => { if (message.type() === "error") result.consoleErrors.push(message.text()); });
    const row = page.locator('tr[data-artifact-id="textus-supervisor"]');
    const button = (action) => row.locator(`button[data-lifecycle-action="${action}"]`);
    async function healthy() {
      should(result.pageErrors.length === 0, `the page must have no fatal errors: ${result.pageErrors.join("; ")}`);
      if (await page.locator("#operational-error").isVisible()) {
        result.operationalError = await page.locator("#operational-error").textContent();
        should(false, `no operational error may be visible: ${result.operationalError}`);
      }
      const errors = await page.locator(".lifecycle-feedback.error:visible").allTextContents();
      const failures = [];
      for (const error of errors) {
        if (/^(Start|Restart) succeeded; runtime is running\. Open App URL is not yet available\.$/.test(error)) {
          if (!result.optionalApplicationDiagnostics.includes(error)) {
            result.optionalApplicationDiagnostics.push(error);
            save();
          }
        } else failures.push(error);
      }
      should(failures.length === 0, `no failed lifecycle feedback may be visible: ${failures.join("; ")}`);
    }
    async function eventually(description, predicate, milliseconds = 70000) {
      const deadline = Date.now() + milliseconds;
      while (Date.now() < deadline) {
        await healthy();
        if (await predicate()) return;
        await delay(50);
      }
      throw new Error(`Then ${description} before its bounded deadline`);
    }
    async function controls(running) {
      return await button("start").isEnabled() === !running &&
        await button("stop").isEnabled() === running && await button("restart").isEnabled() === running;
    }
    async function settled(action, running) {
      await eventually(`${action} must settle with authoritative runtime and restored controls`, async () => {
        const state = await row.getAttribute("data-runtime-state");
        const feedback = await row.locator(".lifecycle-feedback").textContent();
        return (running ? state === "running" : ["stopped", "not-running"].includes(state)) &&
          feedback.includes(`${action} succeeded`) && await controls(running);
      });
    }
    async function lifecycle(action, running) {
      clause("When", `the actual ${action} button is clicked`);
      await button(action.toLowerCase()).click();
      clause("Then", `${action} exposes its busy control state`);
      await eventually(`${action} must expose busy disabled lifecycle controls`, async () =>
        await button("start").isDisabled() && await button("stop").isDisabled() &&
        await button("restart").isDisabled() &&
        await button(action.toLowerCase()).textContent() === ({ Start: "Starting…", Restart: "Restarting…", Stop: "Stopping…" }[action]), 10000);
      clause("Then", `${action} succeeded feedback, runtime and enabled controls agree`);
      await settled(action, running);
    }
    async function screenshot(name) {
      const destination = path.join(path.dirname(receipt), name);
      await page.screenshot({ path: destination, fullPage: true, timeout: 10000 });
      result.screenshots.push(destination);
      save();
    }
    async function exercise() {
      clause("When", "the actual packaged /web/textus-control-center page is opened");
      await page.goto(`${base}/web/textus-control-center`, { waitUntil: "domcontentloaded" });
      clause("Then", "one stopped target has Start enabled and Stop/Restart disabled");
      await eventually("exactly one stopped target row must be ready", async () =>
        await row.count() === 1 && ["stopped", "not-running"].includes(await row.getAttribute("data-runtime-state")) &&
        await controls(false));
      await lifecycle("Start", true);
      await screenshot("panel-running.png");
      clause("Given", "the target row is running after its actual Start");
      clause("When", "the exact target row is clicked to open protected detail");
      await row.click({ position: { x: 8, y: 8 } });
      await page.locator("#operational-detail-dialog").waitFor({ state: "visible" });
      const labels = await page.locator("#operational-detail-fields dt").allTextContents();
      const values = await page.locator("#operational-detail-fields dd").allTextContents();
      clause("Then", "protected detail identifies the actual artifact, runtime, management and DEV sources");
      should(values[labels.indexOf("Artifact ID")] === args.artifact &&
        values[labels.indexOf("Runtime")] === "running" &&
        values[labels.indexOf("Management")] === "auto-managed", "detail must identify artifact, runtime and management");
      const sources = await page.locator("#operational-component-sources").textContent();
      should(sources.includes("DEV") && !sources.includes("No source facts are available."),
        "protected detail must include actual DEV source facts");
      result.detail = { labels, values, sources };
      await page.locator("#close-operational-detail").click();
      await page.locator("#operational-detail-dialog").waitFor({ state: "hidden" });
      clause("Then", "actual artifact/runtime/source detail was inspected and its dialog closed");
      clause("Given", "the started target is still running");
      await lifecycle("Restart", true);
      clause("Given", "the restarted target is running");
      await lifecycle("Stop", false);
      clause("Given", "the stopped target remains under management");
      clause("When", "its actual Remove button is clicked");
      await row.getByRole("button", { name: "Remove", exact: true }).click();
      clause("Then", "the excluded target row is absent");
      await eventually("Remove must omit the target row", async () =>
        await page.locator("#operational-loading").isHidden() && await row.count() === 0);
      clause("When", "the actual Refresh button is clicked");
      const refreshed = page.waitForResponse((response) => response.url().includes("/list-operational-components?") &&
        response.request().method() === "GET", { timeout: 25000 });
      await page.locator("#refresh").click();
      clause("Then", "Refresh preserves exclusion; Restore remains a protected Operation");
      should((await refreshed).ok(), "actual Refresh must complete its management request successfully");
      await eventually("Refresh must finish and preserve exclusion", async () =>
        await page.locator("#operational-loading").isHidden() && await row.count() === 0);
      await screenshot("panel-excluded.png");
      await healthy();
    }
    await Promise.race([exercise(), new Promise((resolve, reject) => {
      watchdog = setTimeout(() => reject(new Error("Then the browser scenario must finish within 225s")), 225000);
    })]);
    result.success = true;
  } catch (error) {
    result.success = false;
    result.error = error.stack || String(error);
    if (receipt && page) {
      try {
        const diagnostic = path.join(path.dirname(receipt), "panel-failure.png");
        await page.screenshot({ path: diagnostic, fullPage: true, timeout: 5000 });
        result.screenshots.push(diagnostic);
      } catch (failure) { result.cleanupErrors.push(`failure screenshot: ${failure.message}`); }
    }
  } finally {
    clearTimeout(watchdog);
    for (const [label, close] of [["private context", () => context?.close()], ["CDP disconnect", () => browser?.close()]]) {
      try { await close(); }
      catch (error) { result.cleanupErrors.push(`${label}: ${error.message}`); result.success = false; }
    }
    if (result.pageErrors.length) { result.success = false; result.error ||= result.pageErrors.join("; "); }
    result.terminal = true;
    if (receipt) save();
  }
  if (!result.success) console.error(result.error || result.cleanupErrors.join("; "));
  return result.success ? 0 : 1;
}

main(process.argv.slice(2)).then((code) => { process.exitCode = code; }, (error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
