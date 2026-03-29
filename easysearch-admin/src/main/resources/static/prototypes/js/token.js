window.QPAdminPages = window.QPAdminPages || {};

window.QPAdminPages.token = function initTokenDictPage() {
  const DEFAULT_NATURE = "NN";
  const VIEW_MODE_VIEW = "view";
  const VIEW_MODE_EDIT = "edit";
  const VIEW_VERSION_ONLINE = "online";

  const data = {
    resourceSets: [
      { id: 301, name: "default-prod", scene: "default", env: "prod", moduleType: "token", currentVersionId: 5102, stagingVersionId: 5103 },
      { id: 302, name: "tradein-prod", scene: "tradein", env: "prod", moduleType: "token", currentVersionId: 6201, stagingVersionId: 6202 }
    ],
    versions: {
      301: [
        { id: 5101, versionNo: 2, status: "archived" },
        { id: 5102, versionNo: 3, status: "published" },
        { id: 5103, versionNo: 4, status: "draft" }
      ],
      302: [
        { id: 6201, versionNo: 1, status: "published" },
        { id: 6202, versionNo: 2, status: "draft" }
      ]
    },
    rules: {
      5103: [
        { id: 1, word: "iPhone15Pro", nature: "nz", frequency: null, enabled: true, selected: false },
        { id: 2, word: "MacBook", nature: "nz", frequency: 100, enabled: true, selected: false },
        { id: 3, word: "以旧换新", nature: "l", frequency: null, enabled: true, selected: false }
      ],
      5102: [
        { id: 101, word: "iPhone15Pro", nature: "nz", frequency: null, enabled: true, selected: false },
        { id: 102, word: "MacBook", nature: "n", frequency: null, enabled: true, selected: false }
      ],
      6201: [],
      6202: []
    }
  };

  const state = {
    resourceSetId: 301,
    viewMode: VIEW_MODE_VIEW,
    viewVersion: VIEW_VERSION_ONLINE,
    versionId: 5102,
    nextRuleId: 30000,
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

  function rowKey(r) {
    return String(r.word || "").trim();
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
      " ｜ qp_rule_token_dict";

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
      const w = (r.word || "").toLowerCase();
      const n = (r.nature || "").toLowerCase();
      return w.indexOf(k) >= 0 || n.indexOf(k) >= 0;
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

  function parseFrequencyInput(val) {
    if (val === "" || val == null) return null;
    const n = Number(val);
    if (!Number.isFinite(n) || n < 0 || Math.floor(n) !== n) return NaN;
    return n;
  }

  function renderTable() {
    const pg = paginatedRules();
    const rows = pg.rows;
    el.tableHeader.innerHTML = "<th><input id='checkAll' type='checkbox'></th><th>#</th><th>word</th><th>nature</th><th>frequency</th><th>enabled</th><th>操作</th>";

    const canEdit = isEditable();
    if (rows.length === 0) {
      el.tableBody.innerHTML = "<tr><td colspan='7' style='text-align:center;color:#5f6b7a;padding:16px;'>暂无词条</td></tr>";
    } else {
      const baseIdx = (state.page - 1) * (Math.max(1, Number(state.pageSize) || 20));
      el.tableBody.innerHTML = rows.map(function (r, idx) {
        const freqVal = r.frequency == null ? "" : String(r.frequency);
        return "<tr" + (r.selected ? " class='selected'" : "") + ">" +
          "<td><input type='checkbox' class='js-select' data-id='" + r.id + "'" + (r.selected ? " checked" : "") + "></td>" +
          "<td>" + (baseIdx + idx + 1) + "</td>" +
          "<td><input class='input js-word' data-id='" + r.id + "' value='" + escapeHtml(r.word) + "'" + (canEdit ? "" : " disabled") + "></td>" +
          "<td><input class='input js-nature' data-id='" + r.id + "' value='" + escapeHtml(r.nature || DEFAULT_NATURE) + "'" + (canEdit ? "" : " disabled") + " style='width:72px;'></td>" +
          "<td><input type='number' min='0' step='1' class='input js-freq' data-id='" + r.id + "' value='" + escapeHtml(freqVal) + "'" + (canEdit ? "" : " disabled") + " style='width:88px;' placeholder=\"空\"></td>" +
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
    el.tableBody.querySelectorAll(".js-word").forEach(function (i) {
      i.addEventListener("input", function (e) { updateRule(Number(e.target.dataset.id), "word", e.target.value); });
    });
    el.tableBody.querySelectorAll(".js-nature").forEach(function (i) {
      i.addEventListener("input", function (e) { updateRule(Number(e.target.dataset.id), "nature", e.target.value); });
    });
    el.tableBody.querySelectorAll(".js-freq").forEach(function (i) {
      i.addEventListener("input", function (e) {
        const raw = e.target.value;
        const n = parseFrequencyInput(raw);
        updateRule(Number(e.target.dataset.id), "frequency", n);
      });
    });
    el.tableBody.querySelectorAll(".js-enabled").forEach(function (i) {
      i.addEventListener("change", function (e) { updateRule(Number(e.target.dataset.id), "enabled", e.target.checked); });
    });
    el.tableBody.querySelectorAll(".js-del").forEach(function (i) {
      i.addEventListener("click", function (e) {
        const id = Number(e.target.dataset.id);
        setRules(getRules().filter(function (r) { return r.id !== id; }));
        addAudit("删除词条 id=" + id + "（versionId=" + state.versionId + "）");
        renderTable();
      });
    });
  }

  function updateRule(id, key, val) {
    const r = getRules().find(function (x) { return x.id === id; });
    if (!r) return;
    if (key === "frequency" && Number.isNaN(val)) return;
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
    const base = {
      id: state.nextRuleId++,
      word: "",
      nature: DEFAULT_NATURE,
      frequency: null,
      enabled: true,
      selected: false
    };
    getRules().push(base);
    addAudit("新增词条 id=" + base.id + "（versionId=" + state.versionId + "）");
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

  function validateRules() {
    const rules = getRules();
    const issues = [];
    const counter = new Map();
    rules.forEach(function (r, idx) {
      const i = idx + 1;
      if (!r.word || !String(r.word).trim()) issues.push("第 " + i + " 行 word 为空");
      if (!r.nature || !String(r.nature).trim()) issues.push("第 " + i + " 行 nature 为空");
      if (r.frequency != null) {
        if (typeof r.frequency !== "number" || r.frequency < 0 || Math.floor(r.frequency) !== r.frequency) {
          issues.push("第 " + i + " 行 frequency 须为非负整数或留空");
        }
      }
      const k = rowKey(r);
      if (k) counter.set(k, (counter.get(k) || 0) + 1);
    });
    counter.forEach(function (v, k) {
      if (v > 1) issues.push("word 重复：" + k + "（共 " + v + " 条）");
    });
    return issues;
  }

  function showValidateResult(issues) {
    const enabledCount = getRules().filter(function (r) { return r.enabled; }).length;
    const list = [];
    list.push("<li class='ok'>词条总数：" + getRules().length + "，启用：" + enabledCount + "</li>");
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
    addPublishLog("发布成功：resourceSet=" + state.resourceSetId + ", online=v" + stagingV.versionNo + ", token_dict");
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
    const f = rule.frequency == null ? "-" : String(rule.frequency);
    return "word=" + (rule.word || "-") + " | nature=" + (rule.nature || "-") + " | freq=" + f +
      " | enabled=" + (!!rule.enabled);
  }

  function compareVersion(baseId) {
    const base = data.rules[baseId] || [];
    const cur = getRules();
    const baseMap = new Map(base.map(function (r) { return [rowKey(r), r]; }));
    const curMap = new Map(cur.map(function (r) { return [rowKey(r), r]; }));

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
      const same = String(b.nature || "") === String(v.nature || "") &&
        (b.frequency == null && v.frequency == null || Number(b.frequency) === Number(v.frequency)) &&
        (!!b.enabled === !!v.enabled);
      if (!same) modified.push({ key: k, base: b, current: v });
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
        return "<div class='diff-line diff-mod'>~ word=" + escapeHtml(m.key) + "</div>" +
          "<div class='diff-line'>  base: " + escapeHtml(ruleLine(m.base)) + "</div>" +
          "<div class='diff-line'>  cur : " + escapeHtml(ruleLine(m.current)) + "</div>";
      }).join("")
      : "<div class='hint'>无变更</div>";

    const baseVersion = (data.versions[state.resourceSetId] || []).find(function (v) { return v.id === baseId; });
    const curVersion = currentVersion();
    el.compareModalTitle.textContent = "版本差异 - v" + (baseVersion ? baseVersion.versionNo : baseId) + " vs v" + (curVersion ? curVersion.versionNo : state.versionId);
    el.compareSummary.textContent = "新增 " + added.length + " 条，删除 " + deleted.length + " 条，变更 " + modified.length + " 条（工作区相对基准）。";
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

  /** 与 DictOnlyTokenizer：正向最长匹配，仅输出词典命中词 */
  function longestWordAt(query, pos, rules) {
    const active = rules.filter(function (r) { return r.enabled && r.word && String(r.word).trim(); });
    let best = null;
    for (let i = 0; i < active.length; i++) {
      const r = active[i];
      const w = r.word;
      if (query.startsWith(w, pos) && (!best || w.length > best.len)) {
        best = { rule: r, len: w.length };
      }
    }
    return best;
  }

  function tokenizeDictOnly(query, rules) {
    const tokens = [];
    let i = 0;
    while (i < query.length) {
      const m = longestWordAt(query, i, rules);
      if (m) {
        tokens.push({
          text: query.slice(i, i + m.len),
          nature: (m.rule.nature && String(m.rule.nature).trim()) ? m.rule.nature : DEFAULT_NATURE,
          start: i,
          end: i + m.len
        });
        i += m.len;
      } else {
        i += 1;
      }
    }
    return tokens;
  }

  function preview() {
    const query = el.previewInput.value;
    if (!query) {
      el.previewOutput.textContent = "请先输入文本";
      return;
    }
    const rules = getRules();
    const tokens = tokenizeDictOnly(query, rules);
    const parts = [
      "输入：<code>" + escapeHtml(query) + "</code>",
      "产出 token 数：" + tokens.length,
      tokens.length
        ? tokens.map(function (t, idx) {
          return "  " + (idx + 1) + ") [" + t.start + "," + t.end + ") <code>" + escapeHtml(t.text) + "</code> / " +
            escapeHtml(t.nature);
        }).join("<br>")
        : "<span class='hint'>（无命中：当前词典未覆盖该串）</span>"
    ].join("<br>");
    el.previewOutput.innerHTML = parts;
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
