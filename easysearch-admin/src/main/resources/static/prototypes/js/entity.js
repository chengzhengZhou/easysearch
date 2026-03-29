window.QPAdminPages = window.QPAdminPages || {};

window.QPAdminPages.entity = function initEntityPage() {
  const ENTITY_TYPES = [
    "CATEGORY", "BRAND", "MODEL", "CPU", "RAM", "STORAGE", "PRICE", "CONDITION",
    "COLOR", "SIZE", "WEIGHT", "BATTERY", "CAMERA", "SCREEN", "OS", "NETWORK",
    "INTERFACE", "FEATURE", "ACCESSORY", "WARRANTY", "TAG", "UNKNOWN"
  ];
  const VIEW_MODE_VIEW = "view";
  const VIEW_MODE_EDIT = "edit";
  const VIEW_VERSION_ONLINE = "online";

  const data = {
    resourceSets: [
      { id: 401, name: "default-prod", scene: "default", env: "prod", moduleType: "entity", currentVersionId: 7102, stagingVersionId: 7103 },
      { id: 402, name: "tradein-prod", scene: "tradein", env: "prod", moduleType: "entity", currentVersionId: 8201, stagingVersionId: 8202 }
    ],
    versions: {
      401: [
        { id: 7101, versionNo: 6, status: "archived" },
        { id: 7102, versionNo: 7, status: "published" },
        { id: 7103, versionNo: 8, status: "draft" }
      ],
      402: [
        { id: 8201, versionNo: 3, status: "published" },
        { id: 8202, versionNo: 4, status: "draft" }
      ]
    },
    rules: {
      7103: [
        { id: 1, entity_text: "苹果", entity_type: "BRAND", normalized_value: "Apple", aliases_json: "[\"apple\",\"iphone\"]", attributes_json: "{\"lang\":\"en\"}", relations_json: "{}", ids_json: "[\"brand_apple\"]", enabled: true, selected: false },
        { id: 2, entity_text: "iPhone15Pro", entity_type: "MODEL", normalized_value: "iPhone 15 Pro", aliases_json: "[\"iphone15pro\",\"15pro\"]", attributes_json: "{\"series\":\"iPhone\"}", relations_json: "{\"brand\":\"Apple\"}", ids_json: "[\"model_iphone15pro\"]", enabled: true, selected: false }
      ],
      7102: [
        { id: 101, entity_text: "苹果", entity_type: "BRAND", normalized_value: "Apple", aliases_json: "[\"apple\"]", attributes_json: "{}", relations_json: "{}", ids_json: "[\"brand_apple\"]", enabled: true, selected: false }
      ],
      8201: [],
      8202: []
    }
  };

  const state = {
    resourceSetId: 401,
    viewMode: VIEW_MODE_VIEW,
    viewVersion: VIEW_VERSION_ONLINE,
    versionId: 7102,
    entityTypeFilter: "",
    nextRuleId: 50000,
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
    entityTypeFilter: document.getElementById("entityTypeFilter"),
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
    pageSizeSelect: document.getElementById("pageSizeSelect"),
    pageInput: document.getElementById("pageInput"),
    tableHeader: document.getElementById("tableHeader"),
    tableBody: document.getElementById("tableBody"),
    previewInput: document.getElementById("previewInput"),
    previewBtn: document.getElementById("previewBtn"),
    previewOutput: document.getElementById("previewOutput"),
    validateList: document.getElementById("validateList"),
    publishLog: document.getElementById("publishLog"),
    auditLog: document.getElementById("auditLog"),
    editModalMask: document.getElementById("editModalMask"),
    editModalTitle: document.getElementById("editModalTitle"),
    editRuleId: document.getElementById("editRuleId"),
    fieldEntityText: document.getElementById("fieldEntityText"),
    fieldEntityType: document.getElementById("fieldEntityType"),
    fieldNormalizedValue: document.getElementById("fieldNormalizedValue"),
    fieldAliases: document.getElementById("fieldAliases"),
    fieldIds: document.getElementById("fieldIds"),
    attrRows: document.getElementById("attrRows"),
    addAttrBtn: document.getElementById("addAttrBtn"),
    formError: document.getElementById("formError"),
    editModalCloseBtn: document.getElementById("editModalCloseBtn"),
    editModalCancelBtn: document.getElementById("editModalCancelBtn"),
    editModalSaveBtn: document.getElementById("editModalSaveBtn"),
    compareModalMask: document.getElementById("compareModalMask"),
    compareModalCloseBtn: document.getElementById("compareModalCloseBtn"),
    compareModalTitle: document.getElementById("compareModalTitle"),
    compareSummary: document.getElementById("compareSummary"),
    diffAdded: document.getElementById("diffAdded"),
    diffDeleted: document.getElementById("diffDeleted"),
    diffModified: document.getElementById("diffModified")
  };

  function escapeHtml(s) {
    const d = document.createElement("div");
    d.textContent = s == null ? "" : String(s);
    return d.innerHTML;
  }
  function safeParse(str, fallback) { try { return JSON.parse(str); } catch (e) { return fallback; } }
  function ruleKey(r) { return String(r.entity_text || "").trim(); }

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

  function getRules() { return data.rules[state.versionId] || []; }
  function setRules(rules) { data.rules[state.versionId] = rules; }

  function addAudit(msg) {
    const now = new Date().toLocaleString();
    state.auditHistory.unshift(now + " | " + msg);
    el.auditLog.innerHTML = state.auditHistory.slice(0, 30).map(escapeHtml).join("<br>");
  }
  function addPublishLog(msg) {
    const now = new Date().toLocaleString();
    state.publishHistory.unshift(now + " | " + msg);
    el.publishLog.innerHTML = state.publishHistory.slice(0, 30).map(escapeHtml).join("<br>");
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
      " ｜ qp_rule_entity";

    el.editabilityHint.textContent = isEditable() ? "工作区可编辑" : "只读查看";
  }

  function renderTypeFilter() {
    el.entityTypeFilter.innerHTML = "<option value=''>全部类型</option>" + ENTITY_TYPES.map(function (t) {
      return "<option value='" + t + "'>" + t + "</option>";
    }).join("");
    el.entityTypeFilter.value = state.entityTypeFilter || "";
  }
  function renderFieldTypeSelect() {
    el.fieldEntityType.innerHTML = ENTITY_TYPES.map(function (t) { return "<option value='" + t + "'>" + t + "</option>"; }).join("");
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
  function closeComparePicker() { el.comparePickerMask.classList.remove("show"); }

  function parseAliasesText(v) {
    return String(v || "").split(/[\n,]+/).map(function (s) { return s.trim(); }).filter(Boolean);
  }
  function parseIdsText(v) {
    return String(v || "").split(",").map(function (s) { return s.trim(); }).filter(Boolean);
  }
  function parseAttrRows() {
    const out = {};
    el.attrRows.querySelectorAll(".attr-row").forEach(function (row) {
      const k = (row.querySelector(".attr-key").value || "").trim();
      const v = row.querySelector(".attr-val").value || "";
      if (k) out[k] = v;
    });
    return out;
  }
  function addAttrRow(k, v) {
    const row = document.createElement("div");
    row.className = "attr-row";
    row.style.display = "flex";
    row.style.gap = "8px";
    row.style.marginBottom = "6px";
    row.innerHTML =
      "<input class='input attr-key' style='flex:1;' placeholder='key' value='" + escapeHtml(k || "") + "'>" +
      "<input class='input attr-val' style='flex:1;' placeholder='value' value='" + escapeHtml(v == null ? "" : String(v)) + "'>" +
      "<button type='button' class='btn danger'>删</button>";
    row.querySelector("button").addEventListener("click", function () { row.remove(); });
    el.attrRows.appendChild(row);
  }
  function fillAttrRows(obj) {
    el.attrRows.innerHTML = "";
    const keys = obj && typeof obj === "object" && !Array.isArray(obj) ? Object.keys(obj) : [];
    if (!keys.length) addAttrRow("", "");
    else keys.forEach(function (k) { addAttrRow(k, obj[k]); });
  }

  function filteredRules() {
    const k = (el.searchInput.value || "").trim().toLowerCase();
    let rows = getRules();
    if (state.entityTypeFilter) rows = rows.filter(function (r) { return (r.entity_type || "").toUpperCase() === state.entityTypeFilter; });
    if (!k) return rows;
    return rows.filter(function (r) {
      const aliasText = (safeParse(r.aliases_json || "[]", []) || []).join(" ").toLowerCase();
      return (r.entity_text || "").toLowerCase().includes(k) ||
        (r.normalized_value || "").toLowerCase().includes(k) ||
        aliasText.includes(k);
    });
  }
  function paginatedRows() {
    const all = filteredRules();
    const size = Math.max(1, Number(state.pageSize) || 20);
    const totalPages = Math.max(1, Math.ceil(all.length / size));
    if (state.page > totalPages) state.page = totalPages;
    const start = (state.page - 1) * size;
    return { rows: all.slice(start, start + size), totalPages: totalPages };
  }

  function renderTable() {
    const pg = paginatedRows();
    const rows = pg.rows;
    const canEdit = isEditable();
    el.tableHeader.innerHTML = "<th><input id='checkAll' type='checkbox'" + (canEdit ? "" : " disabled") + "></th><th>#</th><th>entity</th><th>type</th><th>normalizedValue</th><th>aliases</th><th>attributes</th><th>操作</th>";
    if (!rows.length) {
      el.tableBody.innerHTML = "<tr><td colspan='8' style='text-align:center;color:#5f6b7a;padding:16px;'>暂无数据或筛选无结果</td></tr>";
    } else {
      const baseIdx = (state.page - 1) * (Math.max(1, Number(state.pageSize) || 20));
      el.tableBody.innerHTML = rows.map(function (r, idx) {
        const aliases = safeParse(r.aliases_json || "[]", []);
        const attrs = safeParse(r.attributes_json || "{}", {});
        const aliasText = Array.isArray(aliases) ? aliases.join(", ") : "";
        const attrsText = attrs && typeof attrs === "object" && !Array.isArray(attrs)
          ? Object.keys(attrs).map(function (k) { return k + ":" + attrs[k]; }).join("; ")
          : "";
        return "<tr>" +
          "<td><input type='checkbox' class='js-select' data-id='" + r.id + "'" + (r.selected ? " checked" : "") + (canEdit ? "" : " disabled") + "></td>" +
          "<td>" + (baseIdx + idx + 1) + "</td>" +
          "<td>" + escapeHtml(r.entity_text || "") + "</td>" +
          "<td>" + escapeHtml((r.entity_type || "").toUpperCase()) + "</td>" +
          "<td>" + escapeHtml(r.normalized_value || r.entity_text || "") + "</td>" +
          "<td style='max-width:180px;word-break:break-all;color:#666;'>" + escapeHtml(aliasText) + "</td>" +
          "<td style='max-width:180px;word-break:break-all;color:#666;'>" + escapeHtml(attrsText) + "</td>" +
          "<td class='actions'>" +
          "<button class='btn js-edit' data-id='" + r.id + "'" + (canEdit ? "" : " disabled") + ">编辑</button> " +
          "<button class='btn danger js-del' data-id='" + r.id + "'" + (canEdit ? "" : " disabled") + ">删除</button></td>" +
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

    el.tableBody.querySelectorAll(".js-select").forEach(function (i) {
      i.addEventListener("change", function (e) {
        const r = getRules().find(function (x) { return x.id === Number(e.target.dataset.id); });
        if (!r) return;
        r.selected = e.target.checked;
        refreshButtons();
      });
    });
    el.tableBody.querySelectorAll(".js-edit").forEach(function (btn) {
      btn.addEventListener("click", function () { openEditModal(Number(btn.dataset.id)); });
    });
    el.tableBody.querySelectorAll(".js-del").forEach(function (btn) {
      btn.addEventListener("click", function () {
        const id = Number(btn.dataset.id);
        setRules(getRules().filter(function (r) { return r.id !== id; }));
        addAudit("删除规则 id=" + id + "（versionId=" + state.versionId + "）");
        renderTable();
        showValidateResult(validateRules());
      });
    });

    el.pageInput.value = String(state.page);
    refreshButtons();
  }

  function openEditModal(ruleId) {
    if (!isEditable()) return;
    el.formError.textContent = "";
    renderFieldTypeSelect();
    if (ruleId == null || Number.isNaN(ruleId)) {
      el.editModalTitle.textContent = "新增实体";
      el.editRuleId.value = "";
      el.fieldEntityText.value = "";
      el.fieldEntityType.value = "MODEL";
      el.fieldNormalizedValue.value = "";
      el.fieldAliases.value = "";
      el.fieldIds.value = "";
      fillAttrRows({});
    } else {
      const rule = getRules().find(function (r) { return r.id === ruleId; });
      if (!rule) return;
      el.editModalTitle.textContent = "编辑实体";
      el.editRuleId.value = String(rule.id);
      el.fieldEntityText.value = rule.entity_text || "";
      el.fieldEntityType.value = (rule.entity_type || "MODEL").toUpperCase();
      el.fieldNormalizedValue.value = rule.normalized_value || "";
      const aliases = safeParse(rule.aliases_json || "[]", []);
      el.fieldAliases.value = Array.isArray(aliases) ? aliases.join("\n") : "";
      const ids = safeParse(rule.ids_json || "[]", []);
      el.fieldIds.value = Array.isArray(ids) ? ids.join(",") : "";
      fillAttrRows(safeParse(rule.attributes_json || "{}", {}));
    }
    el.editModalMask.classList.add("show");
  }
  function closeEditModal() { el.editModalMask.classList.remove("show"); }

  function validateAndBuildFormRule() {
    const entity = (el.fieldEntityText.value || "").trim();
    const type = (el.fieldEntityType.value || "").trim().toUpperCase();
    let normalized = (el.fieldNormalizedValue.value || "").trim();
    if (!entity) return { error: "请填写 entity_text（实体原始文本）" };
    if (!type || ENTITY_TYPES.indexOf(type) === -1) return { error: "entity_type 不在允许枚举中，请重新选择" };
    if (!normalized) normalized = entity;

    const aliases = parseAliasesText(el.fieldAliases.value);
    const ids = parseIdsText(el.fieldIds.value);
    const attrs = parseAttrRows();

    return {
      rule: {
        entity_text: entity,
        entity_type: type,
        normalized_value: normalized,
        aliases_json: JSON.stringify(aliases),
        ids_json: JSON.stringify(ids),
        attributes_json: JSON.stringify(attrs)
      }
    };
  }

  function saveEditModal() {
    if (!isEditable()) return;
    const built = validateAndBuildFormRule();
    if (built.error) { el.formError.textContent = built.error; return; }
    const existingId = Number(el.editRuleId.value || 0);
    if (existingId) {
      const r = getRules().find(function (x) { return x.id === existingId; });
      if (!r) return;
      Object.assign(r, built.rule);
      addAudit("编辑规则 id=" + existingId + "（versionId=" + state.versionId + "）");
    } else {
      const base = Object.assign({
        id: state.nextRuleId++,
        enabled: true,
        selected: false,
        relations_json: "{}"
      }, built.rule);
      getRules().push(base);
      addAudit("新增规则 id=" + base.id + "（versionId=" + state.versionId + "）");
    }
    closeEditModal();
    renderTable();
    showValidateResult(validateRules());
  }

  function validateRules() {
    const rules = getRules();
    const issues = [];
    const cnt = new Map();
    rules.forEach(function (r, idx) {
      const i = idx + 1;
      if (!r.entity_text || !String(r.entity_text).trim()) issues.push("第 " + i + " 行 entity_text 为空");
      if (!r.entity_type || ENTITY_TYPES.indexOf(String(r.entity_type).toUpperCase()) === -1) issues.push("第 " + i + " 行 entity_type 非法");
      if (!r.normalized_value || !String(r.normalized_value).trim()) issues.push("第 " + i + " 行 normalized_value 为空");

      const aliases = safeParse(r.aliases_json || "[]", null);
      if (!Array.isArray(aliases)) issues.push("第 " + i + " 行 aliases_json 必须为数组");
      const attrs = safeParse(r.attributes_json || "{}", null);
      if (!attrs || typeof attrs !== "object" || Array.isArray(attrs)) issues.push("第 " + i + " 行 attributes_json 必须为对象");
      const rel = safeParse(r.relations_json || "{}", null);
      if (!rel || typeof rel !== "object" || Array.isArray(rel)) issues.push("第 " + i + " 行 relations_json 必须为对象");
      const ids = safeParse(r.ids_json || "[]", null);
      if (!Array.isArray(ids)) issues.push("第 " + i + " 行 ids_json 必须为数组");

      const k = ruleKey(r);
      if (k) cnt.set(k, (cnt.get(k) || 0) + 1);
    });
    cnt.forEach(function (v, k) { if (v > 1) issues.push("entity_text 重复：" + k + "（共 " + v + " 条）"); });
    return issues;
  }

  function showValidateResult(issues) {
    const enabledCount = getRules().filter(function (r) { return r.enabled; }).length;
    const list = [];
    list.push("<li class='ok'>规则总数：" + getRules().length + "，启用：" + enabledCount + "</li>");
    if (!issues.length) list.push("<li class='ok'>校验通过，可发布</li>");
    else {
      list.push("<li class='warn-text'>校验失败，共 " + issues.length + " 项</li>");
      issues.slice(0, 12).forEach(function (i) { list.push("<li class='err'>" + escapeHtml(i) + "</li>"); });
    }
    el.validateList.innerHTML = list.join("");
  }

  function refreshButtons() {
    const selected = getRules().filter(function (r) { return r.selected; }).length;
    const canEdit = isEditable();
    el.addRuleBtn.disabled = !canEdit;
    el.batchEnableBtn.disabled = !canEdit || selected === 0;
    el.batchDisableBtn.disabled = !canEdit || selected === 0;
    el.batchDeleteBtn.disabled = !canEdit || selected === 0;
    el.publishBtn.disabled = !canEdit;
    el.compareBtn.disabled = !canEdit;
    el.resetStagingBtn.disabled = !canEdit;
    el.validateBtn.disabled = !canEdit;
    el.rollbackBtn.disabled = false;
    el.reloadBtn.disabled = false;
  }

  function applyBatch(action) {
    if (!isEditable()) return;
    getRules().forEach(function (r) {
      if (!r.selected) return;
      if (action === "enable") r.enabled = true;
      if (action === "disable") r.enabled = false;
      if (action === "delete") r._del = true;
    });
    if (action === "delete") setRules(getRules().filter(function (r) { return !r._del; }));
    addAudit("批量操作 " + action + "（versionId=" + state.versionId + "）");
    renderTable();
    showValidateResult(validateRules());
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
    addPublishLog("发布成功：resourceSet=" + state.resourceSetId + ", online=v" + stagingV.versionNo + ", entity");
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

  function ruleLine(r) {
    return "entity=" + (r.entity_text || "-") + " | type=" + (r.entity_type || "-") + " | norm=" + (r.normalized_value || "-") + " | enabled=" + (!!r.enabled);
  }

  function compareVersion(baseId) {
    const base = data.rules[baseId] || [];
    const cur = getRules();
    const baseMap = new Map(base.map(function (r) { return [ruleKey(r), r]; }));
    const curMap = new Map(cur.map(function (r) { return [ruleKey(r), r]; }));
    const added = [];
    const deleted = [];
    const modified = [];
    curMap.forEach(function (v, k) {
      if (!k) return;
      if (!baseMap.has(k)) { added.push(v); return; }
      const b = baseMap.get(k);
      const same = String(b.entity_type || "") === String(v.entity_type || "") &&
        String(b.normalized_value || "") === String(v.normalized_value || "") &&
        String(b.aliases_json || "[]") === String(v.aliases_json || "[]") &&
        String(b.attributes_json || "{}") === String(v.attributes_json || "{}") &&
        String(b.relations_json || "{}") === String(v.relations_json || "{}") &&
        String(b.ids_json || "[]") === String(v.ids_json || "[]") &&
        (!!b.enabled === !!v.enabled);
      if (!same) modified.push({ key: k, base: b, current: v });
    });
    baseMap.forEach(function (v, k) { if (k && !curMap.has(k)) deleted.push(v); });
    el.diffAdded.innerHTML = added.length ? added.map(function (r) { return "<div class='diff-line diff-add'>+ " + escapeHtml(ruleLine(r)) + "</div>"; }).join("") : "<div class='hint'>无新增</div>";
    el.diffDeleted.innerHTML = deleted.length ? deleted.map(function (r) { return "<div class='diff-line diff-del'>- " + escapeHtml(ruleLine(r)) + "</div>"; }).join("") : "<div class='hint'>无删除</div>";
    el.diffModified.innerHTML = modified.length ? modified.map(function (m) {
      return "<div class='diff-line diff-mod'>~ entity_text=" + escapeHtml(m.key) + "</div>" +
        "<div class='diff-line'>  base: " + escapeHtml(ruleLine(m.base)) + "</div>" +
        "<div class='diff-line'>  cur : " + escapeHtml(ruleLine(m.current)) + "</div>";
    }).join("") : "<div class='hint'>无变更</div>";
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

  function previewRecognize(query, rules) {
    const candidates = [];
    rules.filter(function (r) { return r.enabled; }).forEach(function (r) {
      const entity = String(r.entity_text || "").trim();
      if (entity) candidates.push({ text: entity, r: r, viaAlias: false });
      const aliases = safeParse(r.aliases_json || "[]", []);
      if (Array.isArray(aliases)) aliases.forEach(function (a) {
        const t = String(a || "").trim();
        if (t) candidates.push({ text: t, r: r, viaAlias: true });
      });
    });
    const hits = [];
    let i = 0;
    while (i < query.length) {
      let best = null;
      for (let c = 0; c < candidates.length; c++) {
        const item = candidates[c];
        if (query.startsWith(item.text, i) && (!best || item.text.length > best.text.length)) best = item;
      }
      if (!best) { i += 1; continue; }
      hits.push({ start: i, end: i + best.text.length, text: best.text, rule: best.r, viaAlias: best.viaAlias });
      i += best.text.length;
    }
    return hits;
  }

  function preview() {
    const query = el.previewInput.value;
    if (!query) { el.previewOutput.textContent = "请先输入 query"; return; }
    const rows = filteredRules();
    const hits = previewRecognize(query, rows);
    el.previewOutput.innerHTML = [
      "输入：<code>" + escapeHtml(query) + "</code>",
      "识别实体：" + hits.length + " 个",
      hits.length ? hits.map(function (h, idx) {
        return (idx + 1) + ") [" + h.start + "," + h.end + ") <code>" + escapeHtml(h.text) + "</code> => " +
          escapeHtml(h.rule.entity_type) + " / " + escapeHtml(h.rule.normalized_value) + (h.viaAlias ? " <span class='hint'>(alias)</span>" : "");
      }).join("<br>") : "<span class='hint'>无命中</span>"
    ].join("<br>");
  }

  function renderAll() {
    ensureStagingExists();
    state.versionId = resolveViewingVersionId();
    renderTypeFilter();
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
  el.entityTypeFilter.addEventListener("change", function () { state.entityTypeFilter = el.entityTypeFilter.value || ""; state.page = 1; renderTable(); });
  el.searchInput.addEventListener("input", function () { state.page = 1; renderTable(); });

  el.addRuleBtn.addEventListener("click", function () { openEditModal(NaN); });
  el.batchEnableBtn.addEventListener("click", function () { applyBatch("enable"); });
  el.batchDisableBtn.addEventListener("click", function () { applyBatch("disable"); });
  el.batchDeleteBtn.addEventListener("click", function () { applyBatch("delete"); });
  el.pageSizeSelect.addEventListener("change", function () { state.pageSize = Number(el.pageSizeSelect.value) || 20; state.page = 1; renderTable(); });
  el.pageInput.addEventListener("change", function () {
    const max = paginatedRows().totalPages;
    let p = parseInt(el.pageInput.value, 10);
    if (isNaN(p) || p < 1) p = 1;
    if (p > max) p = max;
    state.page = p;
    renderTable();
  });
  el.previewBtn.addEventListener("click", preview);
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
  el.comparePickerMask.addEventListener("click", function (e) { if (e.target === el.comparePickerMask) closeComparePicker(); });
  el.compareModalCloseBtn.addEventListener("click", function () { el.compareModalMask.classList.remove("show"); });
  el.compareModalMask.addEventListener("click", function (e) { if (e.target === el.compareModalMask) el.compareModalMask.classList.remove("show"); });

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

  el.editModalCloseBtn.addEventListener("click", closeEditModal);
  el.editModalCancelBtn.addEventListener("click", closeEditModal);
  el.editModalMask.addEventListener("click", function (e) { if (e.target === el.editModalMask) closeEditModal(); });
  el.addAttrBtn.addEventListener("click", function () { addAttrRow("", ""); });
  el.editModalSaveBtn.addEventListener("click", saveEditModal);

  renderAll();
};
