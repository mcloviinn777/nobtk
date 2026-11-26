// Turns a picked photo into a small square JPEG (data URL) that fits in a Firestore document.
// No server or storage bucket needed: salon photos ~20 KB, client photos ~5 KB.
(function () {
  window.NOBTK_IMG = function (file, size, quality) {
    size = size || 256; quality = quality || 0.8;
    return new Promise(function (resolve, reject) {
      if (!file || !/^image\//.test(file.type || "image/")) return reject(new Error("not-image"));
      var url = URL.createObjectURL(file), img = new Image();
      img.onload = function () {
        try {
          var s = Math.min(img.naturalWidth, img.naturalHeight), c = document.createElement("canvas");
          c.width = c.height = size;
          var g = c.getContext("2d");
          g.fillStyle = "#fff"; g.fillRect(0, 0, size, size);
          g.imageSmoothingQuality = "high";
          g.drawImage(img, (img.naturalWidth - s) / 2, (img.naturalHeight - s) / 2, s, s, 0, 0, size, size);
          var q = quality, out = c.toDataURL("image/jpeg", q);
          while (out.length > size * 260 && q > 0.4) { q -= 0.1; out = c.toDataURL("image/jpeg", q); }
          URL.revokeObjectURL(url); resolve(out);
        } catch (e) { URL.revokeObjectURL(url); reject(e); }
      };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error("unreadable")); };
      img.src = url;
    });
  };
  // Opens the phone's photo picker (gallery or camera) and resolves with the small JPEG, or null if cancelled.
  window.NOBTK_PICK = function (size, quality) {
    return new Promise(function (resolve, reject) {
      var inp = document.createElement("input");
      inp.type = "file"; inp.accept = "image/*"; inp.style.display = "none";
      inp.onchange = function () { var f = inp.files && inp.files[0]; inp.remove(); if (!f) return resolve(null); window.NOBTK_IMG(f, size, quality).then(resolve, reject); };
      document.body.appendChild(inp); inp.click();
    });
  };
})();
