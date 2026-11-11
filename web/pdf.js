// Builds a PDF of a barber's sign-up, in the browser (no server).
// Needs html2canvas and jsPDF loaded before this file.
(function () {
  const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const fmt = (n) => String(n ?? 0).replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  const toDate = (v) => { const d = v && v.toDate ? v.toDate() : (v ? new Date(v) : new Date()); return isNaN(d) ? new Date() : d; };
  const day = (v) => toDate(v).toLocaleDateString("fr-FR", { day: "numeric", month: "long", year: "numeric" });
  const slug = (s) => String(s || "salon").normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z0-9]+/gi, "-").replace(/^-|-$/g, "").toLowerCase() || "salon";

  function html(d) {
    const plan = (d.plan === "solo" ? "Solo" : "Salon") + " · " + (d.chairs || 1) + " fauteuil" + ((d.chairs || 1) > 1 ? "s" : "");
    const row = (l, v) => `<tr><td style="color:#5D5C86;width:38%;padding:7px 0;border-bottom:1px solid #E6E5F3">${l}</td><td style="padding:7px 0;border-bottom:1px solid #E6E5F3;font-weight:600">${v || "–"}</td></tr>`;
    const h2 = (n, t) => `<div style="display:flex;align-items:center;gap:10px;margin:22px 0 8px;font-weight:700;font-size:19px;border-bottom:2px solid #3A31D2;padding-bottom:6px"><span style="width:28px;height:28px;border-radius:8px;background:#FFC53D;color:#2B1E00;display:inline-flex;align-items:center;justify-content:center;font-size:15px">${n}</span>${t}</div>`;
    return `
    <div style="width:794px;padding:48px 52px;box-sizing:border-box;background:#fff;color:#15143A;font-family:Manrope,Arial,sans-serif;font-size:15px;line-height:1.45">
      <div style="display:flex;align-items:center;gap:16px;background:#3A31D2;color:#fff;border-radius:18px;padding:18px 22px">
        <img src="icon-192.png" style="width:60px;height:60px;border-radius:14px" crossorigin="anonymous">
        <div style="flex:1"><div style="font-size:30px;font-weight:800;letter-spacing:1px;line-height:1">NOBTK</div><div style="opacity:.9">Inscription du salon</div></div>
        <div style="text-align:right;font-size:13px"><div style="color:#FFC53D;font-weight:700">2 mois gratuits</div><div>Reçue le ${day(d.createdAt)}</div></div>
      </div>
      ${h2(1, "Le salon")}
      <table style="width:100%;border-collapse:collapse">${row("Nom du salon", esc(d.shop))}${row("Ville", esc(d.city))}${row("Adresse", esc(d.address))}${row("Fauteuils", d.chairs)}${row("Barbiers", d.barbers)}</table>
      ${h2(2, "Le propriétaire")}
      <table style="width:100%;border-collapse:collapse">${row("Nom complet", esc(d.owner))}${row("Téléphone WhatsApp", esc(d.phone))}${row("Email", esc(d.email))}</table>
      ${h2(3, "Services et prix")}
      <table style="width:100%;border-collapse:collapse"><tr style="background:#E9E8F6"><th style="text-align:left;padding:7px 8px">Service</th><th style="text-align:right;padding:7px 8px">Prix</th><th style="text-align:right;padding:7px 8px">Durée</th></tr>
        ${(d.services || []).map((s) => `<tr><td style="padding:7px 8px;border-bottom:1px solid #E6E5F3">${esc(s.name)}</td><td style="padding:7px 8px;border-bottom:1px solid #E6E5F3;text-align:right;font-weight:700">${fmt(s.price)} DH</td><td style="padding:7px 8px;border-bottom:1px solid #E6E5F3;text-align:right">${s.minutes} min</td></tr>`).join("")}</table>
      ${h2(4, "Horaires d'ouverture")}
      <table style="width:100%;border-collapse:collapse">${(d.hours || []).map((h) => row(esc(h.day), h.open ? esc(h.from) + " – " + esc(h.to) : "<span style='color:#B42332'>Fermé</span>")).join("")}</table>
      ${h2(5, "Formule")}
      <div style="display:flex;gap:14px;align-items:stretch">
        <div style="flex:1;background:#3A31D2;color:#fff;border-radius:14px;padding:14px 18px"><div style="opacity:.85">${esc(plan)} · ${d.billing === "year" ? "annuel" : "mensuel"}</div><div style="font-size:28px;font-weight:800;color:#FFC53D">${fmt(d.price)} DH / ${d.billing === "year" ? "an" : "mois"}</div></div>
        <div style="flex:1;background:#FFF0D6;color:#8A5300;border-radius:14px;padding:14px 18px"><div style="font-weight:700">2 mois gratuits</div><div>Premier paiement après le ${d.trialEnds ? day(d.trialEnds) : "–"}. Annulable à tout moment.</div></div>
      </div>
      <table style="width:100%;border-collapse:collapse;margin-top:8px">${row("Comment il a connu NOBTK", esc(d.source))}</table>
      <div style="margin-top:22px;font-size:13px;color:#5D5C86">Le propriétaire accepte que NOBTK le contacte sur WhatsApp pour activer son salon, et que ces informations soient utilisées pour créer son compte.</div>
      <div style="display:flex;gap:16px;margin-top:12px"><div style="flex:1"><div style="font-size:12px;color:#5D5C86">Date</div><div style="height:56px;border:1.5px solid #C9C7E6;border-radius:10px"></div></div><div style="flex:2"><div style="font-size:12px;color:#5D5C86">Signature et cachet</div><div style="height:56px;border:1.5px solid #C9C7E6;border-radius:10px"></div></div></div>
      <div style="margin-top:22px;background:#15143A;color:#fff;border-radius:14px;padding:12px 18px;font-size:13px;display:flex;justify-content:space-between;gap:12px"><span>NOBTK · Ton tour, sans attendre.</span><span>WhatsApp <b style="color:#FFC53D">06 34 36 44 22</b></span></div>
    </div>`;
  }

  // opts.share: open the phone's share sheet (WhatsApp, email…) with the PDF attached when the phone supports it.
  window.NOBTK_PDF = async function (d, opts) {
    opts = opts || {};
    if (!window.html2canvas || !window.jspdf) throw new Error("pdf-lib-missing");
    const wrap = document.createElement("div");
    wrap.style.cssText = "position:fixed;left:-10000px;top:0;z-index:-1";
    wrap.innerHTML = html(d);
    document.body.appendChild(wrap);
    try {
      try { await document.fonts.ready; } catch (e) {}
      const img = wrap.querySelector("img");
      if (img && !img.complete) await new Promise((r) => { img.onload = img.onerror = r; });
      const canvas = await html2canvas(wrap.firstElementChild, { scale: 2, backgroundColor: "#ffffff", useCORS: true });
      const pdf = new window.jspdf.jsPDF({ unit: "mm", format: "a4" });
      const pw = 210, ph = 297, ih = canvas.height * pw / canvas.width;
      const data = canvas.toDataURL("image/jpeg", 0.92);
      let y = 0;
      pdf.addImage(data, "JPEG", 0, 0, pw, ih);
      while (ih - y > ph + 1) { y += ph; pdf.addPage(); pdf.addImage(data, "JPEG", 0, -y, pw, ih); }
      const name = "NOBTK-inscription-" + slug(d.shop) + ".pdf";
      if (opts.share && navigator.canShare) {
        try {
          const file = new File([pdf.output("blob")], name, { type: "application/pdf" });
          if (navigator.canShare({ files: [file] })) { await navigator.share({ files: [file], title: "Inscription NOBTK · " + (d.shop || ""), text: opts.text || "" }); return "shared"; }
        } catch (e) { if (e && e.name === "AbortError") return "cancelled"; }
      }
      if (window.NobtkAndroid && window.NobtkAndroid.saveFile) { window.NobtkAndroid.saveFile(name, pdf.output("datauristring").split(",")[1], "application/pdf"); return "saved"; }
      pdf.save(name); return "saved";
    } finally { wrap.remove(); }
  };
})();
