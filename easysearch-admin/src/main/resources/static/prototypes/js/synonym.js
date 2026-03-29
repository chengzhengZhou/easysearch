window.QPAdminPages = window.QPAdminPages || {};

window.QPAdminPages.synonym = function initSynonymPage() {
  const DIRS = ["=>", "<=", "SYM"];
  const VIEW_MODE_VIEW = "view";
  const VIEW_MODE_EDIT = "edit";
  const VIEW_VERSION_ONLINE = "online";

  const data = {
    resourceSets: [
      { id: 201, name: "default-prod", scene: "default", env: "prod", moduleType: "synonym", currentVersionId: 3002, stagingVersionId: 3003 },
      { id: 202, name: "tradein-prod", scene: "tradein", env: "prod", moduleType: "synonym", currentVersionId: 4001, stagingVersionId: 4002 }
    ],
    versions: {
      201: [
        { id: 3001, versionNo: 4, status: "archived" },
        { id: 3002, versionNo: 5, status: "published" },
        { id: 3003, versionNo: 6, status: "draft" }
      ],
      202: [
        { id: 4001, versionNo: 2, status: "published" },
        { id: 4002, versionNo: 3, status: "draft" }
      ]
    },
    rules: {
      3003: [
        { id: 1, source: "手环", direction: "=>", targetsStr: "手表", enabled: true, selected: false },
        { id: 2, source: "智能手环", direction: "=>", targetsStr: "智能手表", enabled: true, selected: false },
        { id: 3, source: "苹果", direction: "SYM", targetsStr: "iphone,ipad", enabled: true, selected: false }
      ],
      3002: [
        { id: 101, source: "手环", direction: "=>", targetsStr: "手表", enabled: true, selected: false },
        { id: 102, source: "苹果", direction: "SYM", targetsStr: "iphone", enabled: true, selected: false }
      ],
      4001: [],
      4002: []
    }
  };

  const state = {
    resourceSetId: 201,
    viewMode: VIEW_MODE_VIEW,
    viewVersion: VIEW_VERSION_ONLINE,
    versionId: 3002,
    nextRuleId: 20000,
    publishHistory: [],
    auditHistory: [],
    page: 1,
    pageSize: 20
  };

  const el = {
    topContext: document.getElementById("topContext"),
    resourceSetSelect: document.getElementById("resourceSetSelect"),
    viewModeBtn: document.getElementById("viewModeBtn"),
    editModeBtn: document.getElementById("editModeBtn"),
    viewVersionSelect: document.getElementById("viewVersionSelect"),
    viewingVersionText: document.getElementById("viewingVersionText"),
    viewingHintText: document.getElementById("viewingHintText"),
    resetStagingBtn: document.getElementById("resetStagingBtn"),
    validateBtn: document.getElementById("validateBtn"),
    publishBtn: document.getElementById("publishBtn"),
    rollbackBtn: document.getElementById("rollbackBtn"),
    reloadBtn: document.getElementById("reloadBtn"),
    searchInput: document.getElementById("searchInput"),
    addRuleBtn: document.getElementById("addRuleBtn"),
    batchEnableBtn: document.getElementById("batchEnableBtn"),
    batchDisableBtn: document.getElementById("batchDisableBtn"),
    batchDeleteBtn: document.getElementById("batchDeleteBtn"),
    compareBtn: document.getElementById("compareBtn"),
    editabilityHint: document.getElementById("editabilityHint"),
    comparePickerMask: document.getElementById("comparePickerMask"),
    comparePickerSelect: document.getElementById("comparePickerSelect"),
    comparePickerOkBtn: document.getElementById("comparePickerOkBtn"),
    comparePickerCancelBtn: document.getElementById("comparePickerCancelBtn"),
    comparePickerCloseBtn: document.getElementById("comparePickerCloseBtn"),
    rollbackPickerMask: document.getElementById("rollbackPickerMask"),
    rollbackPickerSelect: document.getElementById("rollbackPickerSelect"),
    rollbackPickerOkBtn: document.getElementById("rollbackPickerOkBtn"),
    rollbackPickerCancelBtn: document.getElementById("rollbackPickerCancelBtn"),
    rollbackPickerCloseBtn: document.getElementById("rollbackPickerCloseBtn"),
    publishConfirmMask: document.getElementById("publishConfirmMask"),
    publishConfirmCloseBtn: document.getElementById("publishConfirmCloseBtn"),
    publishConfirmCancelBtn: document.getElementById("publishConfirmCancelBtn"),
    publishConfirmOkBtn: document.getElementById("publishConfirmOkBtn"),
    publishChangeLogInput: document.getElementById("publishChangeLogInput"),
    publishValidateSummary: document.getElementById("publishValidateSummary"),
    tableHeader: document.getElementById("tableHeader"),
    tableBody: document.getElementById("tableBody"),
    previewInput: document.getElementById("previewInput"),
    previewBtn: document.getElementById("previewBtn"),
    previewOutput: document.getElementById("previewOutput"),
    validateList: document.getElementById("validateList"),
    publishLog: document.getElementById("publishLog"),
    auditLog: document.getElementById("auditLog"),
    compareModalMask: document.getElementById("compareModalMask"),
    compareModalCloseBtn: document.getElementById("compareModalCloseBtn"),
    compareModalTitle: document.getElementById("compareModalTitle"),
    compareSummary: document.getElementById("compareSummary"),
    diffAdded: document.getElementById("diffAdded"),
    diffDeleted: document.getElementById("diffDeleted"),
    diffModified: document.getElementById("diffModified"),
    pageSizeSelect: document.getElementById("pageSizeSelect"),
    pageInput: document.getElementById("pageInput")
  };

  function escapeHtml(s) {
    const d = document.createElement("div");
    d.textContent = s == null ? "" : String(s);
    return d.innerHTML;
  }

  function versionsOfSet() {
    return data.versions[state.resourceSetId] || [];
  }
  function currentResourceSet() {
    return data.resourceSets.find(function (r) { return r.id === state.resourceSetId; });
  }
  function currentOnlineVersionId() {
    return currentResourceSet() && currentResourceSet().currentVersionId;
  }
  function currentStagingVersionId() {
    return currentResourceSet() && currentResourceSet().stagingVersionId;
  }
  function currentVersion() {
    return versionsOfSet().find(function (v) { return v.id === state.versionId; });
  }
  function isDraftVersion(versionId) {
    const v = versionsOfSet().find(function (x) { return x.id === versionId; });
    return !!v && v.status === "draft";
  }
  function isEditable() {
    return state.viewMode === VIEW_MODE_EDIT;
  }
  function versionLabel(versionId) {
    const v = versionsOfSet().find(function (x) { return x.id === versionId; });
    if (!v) return "-";
    return "v" + v.versionNo + " (" + v.status + ")";
  }
  function resolveViewingVersionId() {
    const rs = currentResourceSet();
    if (state.viewMode === VIEW_MODE_EDIT) return rs.stagingVersionId;
    if (state.viewVersion !== VIEW_VERSION_ONLINE) return Number(state.viewVersion);
    return rs.currentVersionId;
  }

  function getRules() {
    return data.rules[state.versionId] || [];
  }
  function setRules(rules) {
    data.rules[state.versionId] = rules;
  }

  function addAudit(message) {
    const now = new Date().toLocaleString();
    state.auditHistory.unshift(now + " | " + message);
    el.auditLog.innerHTML = state.auditHistory.slice(0, 30).map(function (i) { return escapeHtml(i); }).join("<br>");
  }
  function addPublishLog(message) {
    const now = new Date().toLocaleString();
    state.publishHistory.unshift(now + " | " + message);
    el.publishLog.innerHTML = state.publishHistory.slice(0, 30).map(function (i) { return escapeHtml(i); }).join("<br>");
  }

  function ensureStagingExists() {
    const rs = currentResourceSet();
    const versions = versionsOfSet();
    if (rs.stagingVersionId && isDraftVersion(rs.stagingVersionId)) return;
    let baseId = rs.currentVersionId;
    if (!baseId) {
      const pub = versions.find(function (v) { return v.status === "published"; });
      baseId = pub ? pub.id : null;
    }
    if (!baseId) return;
    const baseV = versions.find(function (v) { return v.id === baseId; });
    const nextNo = Math.max.apply(null, versions.map(function (v) { return v.versionNo; })) + 1;
    const newId = Number(String(state.resourceSetId) + String(nextNo));
    versions.push({ id: newId, versionNo: nextNo, status: "draft" });
    data.rules[newId] = JSON.parse(JSON.stringify(data.rules[baseId] || []));
    rs.stagingVersionId = newId;
    addAudit("初始化工作区：从线上 v" + (baseV ? baseV.versionNo : baseId) + " 复制为工作区 v" + nextNo);
  }

  function renderResourceSets() {
    el.resourceSetSelect.innerHTML = data.resourceSets.map(function (r) {
      return "<option value='" + r.id + "'>" + r.name + "（scene=" + r.scene + ", env=" + r.env + "）</option>";
    }).join("");
    el.resourceSetSelect.value = String(state.resourceSetId);
  }

  function renderContextBar() {
    const rs = currentResourceSet();
    const onlineId = currentOnlineVersionId();
    const stagingId = currentStagingVersionId();
    const onlineV = versionsOfSet().find(function (v) { return v.id === onlineId; });
    const stagingV = versionsOfSet().find(function (v) { return v.id === stagingId; });

    el.viewModeBtn.classList.toggle("active", state.viewMode === VIEW_MODE_VIEW);
    el.editModeBtn.classList.toggle("active", state.viewMode === VIEW_MODE_EDIT);
    el.viewVersionSelect.disabled = state.viewMode !== VIEW_MODE_VIEW;

    const histories = versionsOfSet()
      .filter(function (v) { return v.status !== "draft"; })
      .sort(function (a, b) { return b.versionNo - a.versionNo; });
    const onlineOption = "<option value='" + VIEW_VERSION_ONLINE + "'>线上当前版本（current_version_id）</option>";
    const historyOptions = histories.length
      ? histories.map(function (v) {
        return "<option value='" + v.id + "'>历史：v" + v.versionNo + " [" + v.status + "]</option>";
      }).join("")
      : "<option value='' disabled>— 无历史版本 —</option>";
    el.viewVersionSelect.innerHTML = onlineOption + historyOptions;

    if (state.viewVersion !== VIEW_VERSION_ONLINE) {
      const vid = Number(state.viewVersion);
      if (!histories.some(function (v) { return v.id === vid; })) state.viewVersion = VIEW_VERSION_ONLINE;
    }
    el.viewVersionSelect.value = String(state.viewVersion);

    const viewingId = resolveViewingVersionId();
    el.viewingVersionText.textContent = versionLabel(viewingId);
    el.viewingHintText.textContent = state.viewMode === VIEW_MODE_EDIT ? "工作区可编辑" : "只读";

    el.topContext.textContent =
      "资源集：" + rs.name +
      " ｜ scene：" + rs.scene +
      " ｜ env：" + rs.env +
      " ｜ 线上：" + (onlineV ? ("v" + onlineV.versionNo) : "-") +
      " ｜ 工作区：" + (stagingV ? ("v" + stagingV.versionNo) : "-") +
      " ｜ qp_rule_synonym";

    el.editabilityHint.textContent = isEditable() ? "工作区可编辑" : "只读查看";
  }

  function renderComparePickerOptions() {
    const onlineId = currentOnlineVersionId();
    const candidates = versionsOfSet().filter(function (v) { return v.status !== "draft"; });
    if (candidates.length === 0) {
      el.comparePickerSelect.innerHTML = "<option value=\"\">— 无线上/历史版本可对比 —</option>";
      return;
    }
    el.comparePickerSelect.innerHTML = candidates.map(function (v) {
      const prefix = v.id === onlineId ? "线上 " : "历史 ";
      return "<option value='" + v.id + "'>" + prefix + "v" + v.versionNo + " [" + v.status + "]</option>";
    }).join("");
    const defaultId = candidates.some(function (v) { return v.id === onlineId; }) ? onlineId : candidates[0].id;
    el.comparePickerSelect.value = String(defaultId);
  }

  function openComparePicker() {
    if (!isEditable()) return;
    renderComparePickerOptions();
    el.comparePickerOkBtn.disabled = !el.comparePickerSelect.value;
    el.comparePickerMask.classList.add("show");
  }

  function closeComparePicker() {
    el.comparePickerMask.classList.remove("show");
  }

  function filteredRulesAll() {
    const k = el.searchInput.value.trim().toLowerCase();
    const all = getRules();
    if (!k) return all;
    return all.filter(function (r) {
      const ts = (r.targetsStr || "").toLowerCase();
      return ((r.source || "").toLowerCase().indexOf(k) >= 0) || ts.indexOf(k) >= 0;
    });
  }

  function paginatedRules() {
    const all = filteredRulesAll();
    const size = Math.max(1, Number(state.pageSize) || 20);
    const totalPages = Math.max(1, Math.ceil(all.length / size));
    if (state.page > totalPages) state.page = totalPages;
    const start = (state.page - 1) * size;
    return { rows: all.slice(start, start + size), total: all.length, totalPages: totalPages };
  }

  function renderTable() {
    const pg = paginatedRules();
    const rows = pg.rows;
    el.tableHeader.innerHTML = "<th><input id='checkAll' type='checkbox'></th><th>#</th><th>source</th><th>direction</th><th>targets（逗号分隔）</th><th>enabled</th><th>操作</th>";

    const canEdit = isEditable();
    if (rows.length === 0) {
      el.tableBody.innerHTML = "<tr><td colspan='7' style='text-align:center;color:#5f6b7a;padding:16px;'>暂无规则</td></tr>";
    } else {
      const baseIdx = (state.page - 1) * (Math.max(1, Number(state.pageSize) || 20));
      el.tableBody.innerHTML = rows.map(function (r, idx) {
        const dirOpts = DIRS.map(function (d) {
          return "<option value='" + d + "'" + (r.direction === d ? " selected" : "") + ">" + d + "</option>";
        }).join("");
        return "<tr" + (r.selected ? " class='selected'" : "") + ">" +
          "<td><input type='checkbox' class='js-select' data-id='" + r.id + "'" + (r.selected ? " checked" : "") + "></td>" +
          "<td>" + (baseIdx + idx + 1) + "</td>" +
          "<td><input class='input js-source' data-id='" + r.id + "' value='" + escapeHtml(r.source) + "'" + (canEdit ? "" : " disabled") + "></td>" +
          "<td><select class='select js-dir' data-id='" + r.id + "'" + (canEdit ? "" : " disabled") + ">" + dirOpts + "</select></td>" +
          "<td><input class='input js-targets' data-id='" + r.id + "' value='" + escapeHtml(r.targetsStr) + "'" + (canEdit ? "" : " disabled") + " style='min-width:180px;'></td>" +
          "<td><input type='checkbox' class='js-enabled' data-id='" + r.id + "'" + (r.enabled ? " checked" : "") + (canEdit ? "" : " disabled") + "></td>" +
          "<td class='actions'><button class='btn js-del' data-id='" + r.id + "'" + (canEdit ? "" : " disabled") + ">删除</button></td>" +
          "</tr>";
      }).join("");
    }

    const checkAll = document.getElementById("checkAll");
    if (checkAll) {
      checkAll.addEventListener("change", function (e) {
        const checked = e.target.checked;
        getRules().forEach(function (r) { r.selected = checked; });
        renderTable();
      });
    }

    bindRuleEvents();
    refreshButtons();
    if (el.pageInput) el.pageInput.value = String(state.page);
  }

  function bindRuleEvents() {
    el.tableBody.querySelectorAll(".js-select").forEach(function (i) {
      i.addEventListener("change", function (e) {
        const r = getRules().find(function (x) { return x.id === Number(e.target.dataset.id); });
        if (!r) return;
        r.selected = e.target.checked;
        refreshButtons();
      });
    });
    el.tableBody.querySelectorAll(".js-source").forEach(function (i) {
      i.addEventListener("input", function (e) { updateRule(Number(e.target.dataset.id), "source", e.target.value); });
    });
    el.tableBody.querySelectorAll(".js-dir").forEach(function (i) {
      i.addEventListener("change", function (e) { updateRule(Number(e.target.dataset.id), "direction", e.target.value); });
    });
    el.tableBody.querySelectorAll(".js-targets").forEach(function (i) {
      i.addEventListener("input", function (e) { updateRule(Number(e.target.dataset.id), "targetsStr", e.target.value); });
    });
    el.tableBody.querySelectorAll(".js-enabled").forEach(function (i) {
      i.addEventListener("change", function (e) { updateRule(Number(e.target.dataset.id), "enabled", e.target.checked); });
    });
    el.tableBody.querySelectorAll(".js-del").forEach(function (i) {
      i.addEventListener("click", function (e) {
        const id = Number(e.target.dataset.id);
        setRules(getRules().filter(function (r) { return r.id !== id; }));
        addAudit("删除规则 id=" + id + "（versionId=" + state.versionId + "）");
        renderTable();
      });
    });
  }

  function updateRule(id, key, val) {
    const r = getRules().find(function (x) { return x.id === id; });
    if (!r) return;
    r[key] = val;
  }

  function refreshButtons() {
    const selectedCount = getRules().filter(function (r) { return r.selected; }).length;
    const canEdit = isEditable();
    el.addRuleBtn.disabled = !canEdit;
    el.batchEnableBtn.disabled = !canEdit || selectedCount === 0;
    el.batchDisableBtn.disabled = !canEdit || selectedCount === 0;
    el.batchDeleteBtn.disabled = !canEdit || selectedCount === 0;
    el.publishBtn.disabled = !canEdit;
    el.compareBtn.disabled = !canEdit;
    el.resetStagingBtn.disabled = !canEdit;
    el.validateBtn.disabled = !canEdit;
    el.rollbackBtn.disabled = false;
    el.reloadBtn.disabled = false;
  }

  function addRule() {
    if (!isEditable()) return;
    const base = { id: state.nextRuleId++, source: "", direction: "=>", targetsStr: "", enabled: true, selected: false };
    getRules().push(base);
    addAudit("新增规则 id=" + base.id + "（versionId=" + state.versionId + "）");
    state.page = Math.ceil(filteredRulesAll().length / Math.max(1, Number(state.pageSize) || 20)) || 1;
    renderTable();
  }

  function applyBatch(action) {
    if (!isEditable()) return;
    getRules().forEach(function (r) {
      if (!r.selected) return;
      if (action === "enable") r.enabled = true;
      if (action === "disable") r.enabled = false;
    });
    addAudit("批量操作 " + action + "（versionId=" + state.versionId + "）");
    renderTable();
  }

  function deleteSelected() {
    if (!isEditable()) return;
    const before = getRules().length;
    setRules(getRules().filter(function (r) { return !r.selected; }));
    addAudit("批量删除 " + (before - getRules().length) + " 条（versionId=" + state.versionId + "）");
    renderTable();
  }

  function parseTargets(str) {
    if (!str || !String(str).trim()) return [];
    return String(str).split(",").map(function (s) { return s.trim(); }).filter(Boolean);
  }

  function validateRules() {
    const rules = getRules();
    const issues = [];
    const counter = new Map();
    rules.forEach(function (r, idx) {
      const i = idx + 1;
      if (!r.source || !String(r.source).trim()) issues.push("第 " + i + " 行 source 为空");
      if (DIRS.indexOf(r.direction) === -1) issues.push("第 " + i + " 行 direction 须为 =>、<= 或 SYM");
      const targets = parseTargets(r.targetsStr);
      if (targets.length === 0) issues.push("第 " + i + " 行 targets 为空");
      const k = String(r.source || "").trim();
      if (k) counter.set(k, (counter.get(k) || 0) + 1);
    });
    counter.forEach(function (v, k) {
      if (v > 1) issues.push("source 重复：" + k + "（共 " + v + " 条）");
    });
    return issues;
  }

  function showValidateResult(issues) {
    const enabledCount = getRules().filter(function (r) { return r.enabled; }).length;
    const list = [];
    list.push("<li class='ok'>规则总数：" + getRules().length + "，启用：" + enabledCount + "</li>");
    if (issues.length === 0) {
      list.push("<li class='ok'>校验通过，可发布</li>");
    } else {
      list.push("<li class='warn-text'>校验失败，共 " + issues.length + " 项</li>");
      issues.slice(0, 12).forEach(function (i) { list.push("<li class='err'>" + escapeHtml(i) + "</li>"); });
    }
    el.validateList.innerHTML = list.join("");
  }

  function publish() {
    if (!isEditable()) return;
    openPublishConfirm();
  }

  function openPublishConfirm() {
    const issues = validateRules();
    showValidateResult(issues);
    if (issues.length === 0) {
      el.publishValidateSummary.textContent = "校验：通过（可发布）";
    } else {
      el.publishValidateSummary.textContent = "校验：失败（" + issues.length + " 项），不可发布";
    }
    el.publishConfirmOkBtn.disabled = issues.length > 0;
    el.publishConfirmMask.classList.add("show");
    try { el.publishChangeLogInput.focus(); } catch (e) {}
  }

  function closePublishConfirm() {
    el.publishConfirmMask.classList.remove("show");
  }

  function confirmPublish() {
    const changeLog = (el.publishChangeLogInput.value || "").trim();
    if (!changeLog) {
      alert("请填写发布说明（change_log）");
      try { el.publishChangeLogInput.focus(); } catch (e) {}
      return;
    }
    const issues = validateRules();
    showValidateResult(issues);
    if (issues.length > 0) {
      alert("校验不通过，不能发布");
      el.publishConfirmOkBtn.disabled = true;
      el.publishValidateSummary.textContent = "校验：失败（" + issues.length + " 项），不可发布";
      return;
    }
    closePublishConfirm();

    const rs = currentResourceSet();
    const versions = versionsOfSet();
    const stagingId = rs.stagingVersionId;
    const stagingV = versions.find(function (v) { return v.id === stagingId; });
    if (!stagingV || stagingV.status !== "draft") {
      alert("工作区版本异常，无法发布（模拟）");
      return;
    }

    versions.forEach(function (v) { if (v.status === "published") v.status = "archived"; });
    stagingV.status = "published";
    rs.currentVersionId = stagingId;
    addPublishLog("发布成功：resourceSet=" + state.resourceSetId + ", online=v" + stagingV.versionNo + ", synonym");
    addAudit("发布：current_version_id=" + stagingId + "，changeLog=" + changeLog);

    const nextNo = Math.max.apply(null, versions.map(function (v) { return v.versionNo; })) + 1;
    const newDraftId = Number(String(state.resourceSetId) + String(nextNo));
    versions.push({ id: newDraftId, versionNo: nextNo, status: "draft" });
    data.rules[newDraftId] = JSON.parse(JSON.stringify(data.rules[rs.currentVersionId] || []));
    rs.stagingVersionId = newDraftId;

    state.versionId = rs.stagingVersionId;
    el.publishChangeLogInput.value = "";
    renderAll();
  }

  function rollback() {
    openRollbackPicker();
  }

  function renderRollbackPickerOptions() {
    const onlineId = currentOnlineVersionId();
    const candidates = versionsOfSet().filter(function (v) { return v.status !== "draft" && v.id !== onlineId; });
    if (candidates.length === 0) {
      el.rollbackPickerSelect.innerHTML = "<option value=\"\">— 无可回滚版本 —</option>";
      return;
    }
    const sorted = candidates.slice().sort(function (a, b) { return b.versionNo - a.versionNo; });
    el.rollbackPickerSelect.innerHTML = sorted.map(function (v) {
      return "<option value='" + v.id + "'>v" + v.versionNo + " [" + v.status + "]</option>";
    }).join("");
    el.rollbackPickerSelect.value = String(sorted[0].id);
  }

  function openRollbackPicker() {
    renderRollbackPickerOptions();
    el.rollbackPickerOkBtn.disabled = !el.rollbackPickerSelect.value;
    el.rollbackPickerMask.classList.add("show");
  }

  function closeRollbackPicker() {
    el.rollbackPickerMask.classList.remove("show");
  }

  function confirmRollback() {
    const raw = el.rollbackPickerSelect.value;
    if (!raw) {
      alert("暂无可回滚版本");
      return;
    }
    const toVersionId = Number(raw);
    const versions = versionsOfSet();
    const target = versions.find(function (v) { return v.id === toVersionId; });
    if (!target || target.status === "draft") {
      alert("版本选择非法");
      return;
    }
    closeRollbackPicker();

    versions.forEach(function (v) { if (v.status === "published") v.status = "archived"; });
    target.status = "published";
    currentResourceSet().currentVersionId = target.id;
    addPublishLog("回滚成功：切换到 v" + target.versionNo);
    addAudit("回滚至 versionId=" + target.id + "，并同步重置工作区=线上");
    resetStagingToOnline(true);
    renderAll();
  }

  function reload() {
    addPublishLog("已触发 Reload（模拟）：resourceSet=" + state.resourceSetId + ", versionId=" + state.versionId);
    alert("已触发实例 Reload（模拟）");
  }

  function resetStagingToOnline(silent) {
    const rs = currentResourceSet();
    const versions = versionsOfSet();
    const onlineId = rs.currentVersionId;
    const onlineV = versions.find(function (v) { return v.id === onlineId; });
    if (!onlineId || !onlineV) {
      alert("线上版本不存在（模拟）");
      return;
    }
    const oldStagingId = rs.stagingVersionId;
    const oldV = versions.find(function (v) { return v.id === oldStagingId; });
    if (oldV && oldV.status === "draft") oldV.status = "archived";

    const nextNo = Math.max.apply(null, versions.map(function (v) { return v.versionNo; })) + 1;
    const newId = Number(String(state.resourceSetId) + String(nextNo));
    versions.push({ id: newId, versionNo: nextNo, status: "draft" });
    data.rules[newId] = JSON.parse(JSON.stringify(data.rules[onlineId] || []));
    rs.stagingVersionId = newId;
    state.versionId = newId;
    if (!silent) addAudit("重置工作区=线上 v" + onlineV.versionNo + " -> 新工作区 v" + nextNo);
  }

  function ruleLine(rule) {
    return "source=" + (rule.source || "-") + " | dir=" + (rule.direction || "=>") + " | targets=" + (rule.targetsStr || "-") + " | enabled=" + (!!rule.enabled);
  }

  function compareVersion(baseId) {
    const base = data.rules[baseId] || [];
    const cur = getRules();
    const baseMap = new Map(base.map(function (r) { return [String(r.source || "").trim(), r]; }));
    const curMap = new Map(cur.map(function (r) { return [String(r.source || "").trim(), r]; }));

    const added = [];
    const deleted = [];
    const modified = [];

    curMap.forEach(function (v, k) {
      if (!k) return;
      if (!baseMap.has(k)) {
        added.push(v);
        return;
      }
      const b = baseMap.get(k);
      const same = (b.direction === v.direction) && (String(b.targetsStr || "") === String(v.targetsStr || "")) && (!!b.enabled === !!v.enabled);
      if (!same) modified.push({ source: k, base: b, current: v });
    });
    baseMap.forEach(function (v, k) {
      if (!k) return;
      if (!curMap.has(k)) deleted.push(v);
    });

    el.diffAdded.innerHTML = added.length
      ? added.map(function (r) { return "<div class='diff-line diff-add'>+ " + escapeHtml(ruleLine(r)) + "</div>"; }).join("")
      : "<div class='hint'>无新增</div>";

    el.diffDeleted.innerHTML = deleted.length
      ? deleted.map(function (r) { return "<div class='diff-line diff-del'>- " + escapeHtml(ruleLine(r)) + "</div>"; }).join("")
      : "<div class='hint'>无删除</div>";

    el.diffModified.innerHTML = modified.length
      ? modified.map(function (m) {
        return "<div class='diff-line diff-mod'>~ source=" + escapeHtml(m.source) + "</div>" +
          "<div class='diff-line'>  base: " + escapeHtml(ruleLine(m.base)) + "</div>" +
          "<div class='diff-line'>  cur : " + escapeHtml(ruleLine(m.current)) + "</div>";
      }).join("")
      : "<div class='hint'>无变更</div>";

    const baseVersion = (data.versions[state.resourceSetId] || []).find(function (v) { return v.id === baseId; });
    const curVersion = currentVersion();
    el.compareModalTitle.textContent = "版本差异 - v" + (baseVersion ? baseVersion.versionNo : baseId) + " vs v" + (curVersion ? curVersion.versionNo : state.versionId);
    el.compareSummary.textContent = "新增 " + added.length + " 条，删除 " + deleted.length + " 条，变更 " + modified.length + " 条。";
    el.compareModalMask.classList.add("show");
    addAudit("版本对比 v" + (baseVersion ? baseVersion.versionNo : baseId) + " vs v" + (curVersion ? curVersion.versionNo : state.versionId));
  }

  function confirmCompare() {
    const raw = el.comparePickerSelect.value;
    if (!raw) {
      alert("暂无可对比版本");
      return;
    }
    const baseId = Number(raw);
    closeComparePicker();
    compareVersion(baseId);
  }

  function longestMatchAt(query, pos, rules) {
    const active = rules.filter(function (r) { return r.enabled && r.source; });
    let best = null;
    for (let i = 0; i < active.length; i++) {
      const r = active[i];
      const s = r.source;
      if (query.startsWith(s, pos) && (!best || s.length > best.len)) {
        best = { rule: r, len: s.length };
      }
    }
    return best;
  }

  function rewriteReplaceFirst(query, rules) {
    let i = 0;
    while (i < query.length) {
      const m = longestMatchAt(query, i, rules);
      if (m) {
        const t = parseTargets(m.rule.targetsStr);
        const first = t[0] || "";
        return query.slice(0, i) + first + query.slice(i + m.len);
      }
      i++;
    }
    return query;
  }

  function collectMatchesNonOverlap(query, rules) {
    const hits = [];
    let i = 0;
    while (i < query.length) {
      const m = longestMatchAt(query, i, rules);
      if (m) {
        hits.push({ start: i, len: m.len, rule: m.rule });
        i += m.len;
      } else {
        i++;
      }
    }
    return hits;
  }

  function preview() {
    const query = el.previewInput.value;
    if (!query) {
      el.previewOutput.textContent = "请先输入 query";
      return;
    }
    const rules = getRules();
    const out = rewriteReplaceFirst(query, rules);
    const matches = collectMatchesNonOverlap(query, rules);
    const lines = [
      "输入：<code>" + escapeHtml(query) + "</code>",
      "非重叠最长匹配：" + matches.length + " 处",
      matches.length ? matches.map(function (h, idx) {
        return "  " + (idx + 1) + ") [" + h.start + "," + (h.start + h.len) + ") \"" + escapeHtml(query.slice(h.start, h.start + h.len)) + "\" → " + escapeHtml(h.rule.direction) + " " + escapeHtml(h.rule.targetsStr || "");
      }).join("<br>") : "",
      "ReplaceFirst 输出：<code>" + escapeHtml(out) + "</code>"
    ].filter(Boolean).join("<br>");
    el.previewOutput.innerHTML = lines;
  }

  function renderAll() {
    ensureStagingExists();
    state.versionId = resolveViewingVersionId();
    renderResourceSets();
    renderContextBar();
    renderTable();
    showValidateResult(validateRules());
    refreshButtons();
    if (!el.publishLog.innerHTML) addPublishLog("等待发布操作...");
    if (!el.auditLog.innerHTML) addAudit("页面初始化完成");
  }

  el.resourceSetSelect.addEventListener("change", function (e) {
    state.resourceSetId = Number(e.target.value);
    state.viewMode = VIEW_MODE_VIEW;
    state.viewVersion = VIEW_VERSION_ONLINE;
    state.page = 1;
    addAudit("切换资源集 resourceSetId=" + state.resourceSetId);
    renderAll();
  });
  el.viewModeBtn.addEventListener("click", function () {
    state.viewMode = VIEW_MODE_VIEW;
    state.viewVersion = VIEW_VERSION_ONLINE;
    addAudit("切换模式：查看（线上）");
    renderAll();
  });
  el.editModeBtn.addEventListener("click", function () {
    state.viewMode = VIEW_MODE_EDIT;
    addAudit("切换模式：编辑（进入工作区）");
    renderAll();
  });
  el.viewVersionSelect.addEventListener("change", function (e) {
    state.viewVersion = e.target.value;
    state.page = 1;
    addAudit("切换查看版本：" + state.viewVersion);
    renderAll();
  });
  el.searchInput.addEventListener("input", function () { state.page = 1; renderTable(); });

  el.addRuleBtn.addEventListener("click", addRule);
  el.batchEnableBtn.addEventListener("click", function () { applyBatch("enable"); });
  el.batchDisableBtn.addEventListener("click", function () { applyBatch("disable"); });
  el.batchDeleteBtn.addEventListener("click", deleteSelected);
  el.validateBtn.addEventListener("click", function () {
    const issues = validateRules();
    showValidateResult(issues);
    alert(issues.length ? ("校验失败：\n- " + issues.join("\n- ")) : "校验通过");
  });
  el.publishBtn.addEventListener("click", publish);
  el.rollbackBtn.addEventListener("click", rollback);
  el.reloadBtn.addEventListener("click", reload);
  el.compareBtn.addEventListener("click", openComparePicker);
  el.resetStagingBtn.addEventListener("click", function () {
    if (!isEditable()) return;
    if (!confirm("将重置工作区为线上版本，未发布变更将丢失。继续？")) return;
    resetStagingToOnline(false);
    renderAll();
  });
  el.comparePickerOkBtn.addEventListener("click", confirmCompare);
  el.comparePickerCancelBtn.addEventListener("click", closeComparePicker);
  el.comparePickerCloseBtn.addEventListener("click", closeComparePicker);
  el.comparePickerMask.addEventListener("click", function (e) {
    if (e.target === el.comparePickerMask) closeComparePicker();
  });
  el.previewBtn.addEventListener("click", preview);
  el.compareModalCloseBtn.addEventListener("click", function () { el.compareModalMask.classList.remove("show"); });
  el.compareModalMask.addEventListener("click", function (e) {
    if (e.target === el.compareModalMask) el.compareModalMask.classList.remove("show");
  });

  el.rollbackPickerOkBtn.addEventListener("click", confirmRollback);
  el.rollbackPickerCancelBtn.addEventListener("click", closeRollbackPicker);
  el.rollbackPickerCloseBtn.addEventListener("click", closeRollbackPicker);
  el.rollbackPickerMask.addEventListener("click", function (e) {
    if (e.target === el.rollbackPickerMask) closeRollbackPicker();
  });

  el.publishConfirmOkBtn.addEventListener("click", confirmPublish);
  el.publishConfirmCancelBtn.addEventListener("click", closePublishConfirm);
  el.publishConfirmCloseBtn.addEventListener("click", closePublishConfirm);
  el.publishConfirmMask.addEventListener("click", function (e) {
    if (e.target === el.publishConfirmMask) closePublishConfirm();
  });

  if (el.pageSizeSelect) {
    el.pageSizeSelect.addEventListener("change", function () {
      state.pageSize = Number(el.pageSizeSelect.value) || 20;
      state.page = 1;
      renderTable();
    });
  }
  if (el.pageInput) {
    el.pageInput.addEventListener("change", function () {
      const pg = paginatedRules();
      let p = parseInt(el.pageInput.value, 10);
      if (isNaN(p) || p < 1) p = 1;
      if (p > pg.totalPages) p = pg.totalPages;
      state.page = p;
      renderTable();
    });
  }

  renderAll();
};
