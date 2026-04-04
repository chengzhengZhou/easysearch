/**
 * QP Admin 原型壳：Hash 路由 + fetch 注入页面片段 + 动态加载页面脚本
 * 路由格式：index.html#/intervention 、 #/synonym
 */
(function () {
  "use strict";

  var ROUTES = {
    overview: { title: "总览", html: "pages/overview.html", pageKey: null },
    intervention: { title: "干预规则", html: "pages/intervention.html", pageKey: "intervention" },
    synonym: { title: "同义词", html: "pages/synonym.html", pageKey: "synonym" },
    entity: { title: "实体词典", html: "pages/entity.html", pageKey: "entity" },
    token: { title: "分词词典", html: "pages/token.html", pageKey: "token" },
    meta: { title: "元信息", html: "pages/meta.html", pageKey: "meta" },
    publish: { title: "发布记录", html: "pages/publish.html", pageKey: null },
    audit: { title: "操作审计", html: "pages/audit.html", pageKey: null }
  };

  var DEFAULT_ROUTE = "intervention";

  function baseUrl() {
    var path = window.location.pathname;
    var i = path.lastIndexOf("/");
    return path.slice(0, i + 1);
  }

  function parseRoute() {
    var h = (window.location.hash || "").replace(/^#\/?/, "").trim();
    var name = h.split("/")[0] || DEFAULT_ROUTE;
    if (!ROUTES[name]) name = DEFAULT_ROUTE;
    return name;
  }

  function setNavActive(routeName) {
    document.querySelectorAll(".nav-item[data-route]").forEach(function (a) {
      a.classList.toggle("active", a.getAttribute("data-route") === routeName);
    });
  }

  function removePageScript() {
    var old = document.getElementById("qp-admin-page-script");
    if (old) old.remove();
  }

  function loadPageScript(pageKey, onDone) {
    removePageScript();
    if (!pageKey || !window.QPAdminPages || typeof window.QPAdminPages[pageKey] !== "function") {
      if (onDone) onDone();
      return;
    }
    try {
      window.QPAdminPages[pageKey]();
    } catch (e) {
      console.error("QPAdminPages." + pageKey + " failed", e);
    }
    if (onDone) onDone();
  }

  /**
   * 动态加载外部 JS（用于首次进入带 pageKey 的页面）
   */
  function injectScript(pageKey, onDone) {
    removePageScript();
    if (!pageKey) {
      if (onDone) onDone();
      return;
    }
    var s = document.createElement("script");
    s.id = "qp-admin-page-script";
    s.async = true;
    s.src = baseUrl() + "js/" + pageKey + ".js?t=" + Date.now();
    s.onload = function () {
      loadPageScript(pageKey, onDone);
    };
    s.onerror = function () {
      console.error("Failed to load script: " + s.src);
      if (onDone) onDone();
    };
    document.body.appendChild(s);
  }

  var navigating = false;

  function navigate() {
    if (navigating) return;
    navigating = true;
    var routeName = parseRoute();
    var def = ROUTES[routeName];
    var mount = document.getElementById("page-mount");
    if (!mount || !def) {
      navigating = false;
      return;
    }

    setNavActive(routeName);
    document.title = "QP Admin - " + def.title;

    var url = baseUrl() + def.html;
    fetch(url, { cache: "no-store" })
      .then(function (r) {
        if (!r.ok) throw new Error(r.status + " " + url);
        return r.text();
      })
      .then(function (html) {
        mount.innerHTML = html;
        if (def.pageKey) {
          if (window.QPAdminPages && typeof window.QPAdminPages[def.pageKey] === "function") {
            loadPageScript(def.pageKey, function () { navigating = false; });
          } else {
            injectScript(def.pageKey, function () { navigating = false; });
          }
        } else {
          navigating = false;
        }
      })
      .catch(function (err) {
        console.error(err);
        mount.innerHTML = "<div class=\"panel\"><p class=\"err\">加载失败：" + String(err.message) + "</p></div>";
        navigating = false;
      });
  }

  window.addEventListener("hashchange", navigate);

  document.addEventListener("DOMContentLoaded", function () {
    if (!window.location.hash || window.location.hash === "#") {
      window.location.hash = "#/" + DEFAULT_ROUTE;
      return;
    }
    navigate();
  });
})();
