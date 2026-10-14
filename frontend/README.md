# Online banking frontend

## Run locally

1. Start MySQL and the Spring Boot API in `backend`. The API uses the defaults in `backend/src/main/resources/application.properties` unless the `BANK_DB_*` environment variables override them.
2. Run `npm install` and `npm run dev` from this directory.
3. Open <http://localhost:5173>. Vite proxies `/api` requests to the API on port 8080 so the browser's session and CSRF cookies are shared with the frontend.

Register a test customer or sign in with an existing test account. The dashboard displays live account and transaction data, and supports creating accounts, recording deposits, and transferring money. All monetary values are displayed in Indian rupees (INR). This educational application is not connected to a real bank or payment network.
