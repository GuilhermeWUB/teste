from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch()
    page = browser.new_page()

    # Navega para a página de cadastro de veículos
    page.goto("http://localhost:8080/vehicles/new")

    # Preenche o código FIPE e clica em buscar
    page.fill("#codigo_fipe", "005340-6")
    page.click("button[value='buscarFipe']")

    # Espera que o campo de valor da FIPE seja preenchido
    page.wait_for_selector("#fipe_value")

    # Tira uma captura de tela
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)
