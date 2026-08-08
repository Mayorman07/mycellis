\# TECH\_DEBT.md



\## Post-launch refactors



\- \*\*StalkTable/StalkRow dual-mount pattern:\*\* Currently renders each stalk 

&#x20; twice in DOM (desktop table row + mobile card) with `hidden md:block` / 

&#x20; `md:hidden`. Fine for < 100 stalks per org. Refactor to single-mount when 

&#x20; we add row selection, dropdowns per row, inline editing, or virtualization.

&#x20; Options: useMediaQuery + hydration-safe fallback, react-responsive, or 

&#x20; headless table lib (@tanstack/react-table).



\- \*\*DashboardHeader "Overview" tab:\*\* hardcoded active, doesn't navigate. 

&#x20; Fix to actually route to /dashboard and use useLocation for active state.



\- \*\*Desktop StalkRow keyboard operability:\*\* currently click-only. Add 

&#x20; role="button", tabIndex, keyboard handler for consistency with mobile 

&#x20; card. Post-launch a11y polish.



\- \*\*`mycelis.io` typo:\*\* Every backend error response has 

&#x20; `"type":"https://mycelis.io/errors/..."` — single L. Fix in 

&#x20; application-prod.properties or wherever the problem-detail URI base 

&#x20; is configured.



\- \*\*Two-machine deploy:\*\* Currently on `fly scale count 1` due to in-memory 

&#x20; session store. Add Spring Session + Upstash Redis to enable multi-machine 

&#x20; scaling. Free tier of Upstash on Fly Redis is sufficient.



\- \*\*Rotate admin password:\*\* olajidemayorwa@gmail.com password was 

&#x20; compromised during launch debugging session (Aug 8, 2026). Change 

&#x20; via app UI, and update MYCELIS\_ADMIN\_PASSWORD Fly secret.

