
import { Locator, Page } from "@playwright/test";
import { environment } from "../../src/environments/environment";

export const signUPURL = `${environment.signUp_URL}`;

export class SignUpPage {
    readonly page: Page;
    readonly emailInput: Locator;
    readonly passwordInput: Locator
    readonly signUpButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.emailInput = page.locator('#email');
        this.passwordInput = page.locator('#password');
        this.signUpButton = page.getByRole('button', { name: 'signUP' });
    }

    async goToSignUpPage(){
        await this.page.goto(signUPURL);
    }

    async signUpUser(emailInput: string, password: string) {
        await this.emailInput.fill(emailInput);
        await this.passwordInput.fill(password);
        await this.signUpButton.click();
    }
}
