// Boot
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

initTheme();
initPrivacy();
(function boot(){applyPerformanceMode();const s=localStorage.getItem(CK);if(s){cfg=JSON.parse(s);initApp();}else showSetup();})();
