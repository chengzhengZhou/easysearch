window.QPAdminPages = window.QPAdminPages || {};
window.QPAdminPages.intervention = function initInterventionPage() {
    const MODE_SENTENCE = "sentence";
    const MODE_TERM = "term";
    const VIEW_MODE_VIEW = "view";
    const VIEW_MODE_EDIT = "edit";
    const VIEW_VERSION_ONLINE = "online";

    const data = {
      resourceSets: [
        { id: 101, name: "default-prod", scene: "default", env: "prod", moduleType: "intervention", currentVersionId: 1002, stagingVersionId: 1003 },
        { id: 102, name: "tradein-prod", scene: "tradein", env: "prod", moduleType: "intervention", currentVersionId: 2001, stagingVersionId: 2002 }
      ],
      versions: {
        101: [
          { id: 1001, versionNo: 10, status: "archived" },
          { id: 1002, versionNo: 11, status: "published" },  // current
          { id: 1003, versionNo: 12, status: "draft" }       // staging
        ],
        102: [
          { id: 2001, versionNo: 7, status: "published" },   // current
          { id: 2002, versionNo: 8, status: "draft" }        // staging
        ]
      },
      rules: {
        "1003:sentence": [
          { id: 1, source: "苹果手机多少钱", target: "iphone 价格", matchType: "EXACT", priority: 10, enabled: true, selected: false },
          { id: 2, source: "华为换机", target: "huawei 以旧换新", matchType: "PREFIX", priority: 6, enabled: true, selected: false }
        ],
        "1003:term": [
          { id: 11, source: "苹果", target: "iphone", priority: 3, enabled: true, selected: false },
          { id: 12, source: "华为", target: "huawei", priority: 2, enabled: true, selected: false }
        ],
        "1002:sentence": [
          { id: 101, source: "苹果手机多少钱", target: "苹果 价格", matchType: "EXACT", priority: 8, enabled: true, selected: false }
        ],
        "1002:term": [
          { id: 111, source: "苹果", target: "apple", priority: 1, enabled: true, selected: false }
        ],
        "2002:sentence": [],
        "2002:term": []
      }
    };

    const state = {
      resourceSetId: 101,
      mode: MODE_SENTENCE,
      viewMode: VIEW_MODE_VIEW,          // view=只读查看；edit=进入工作区编辑
      viewVersion: VIEW_VERSION_ONLINE,  // online 或具体 versionId（仅 viewMode=view 有意义）
      versionId: 1002,                   // 当前列表/预览使用的版本（由 viewMode/viewTarget 推导）
      nextRuleId: 10000,
      publishHistory: [],
      auditHistory: []
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
      sentenceChip: document.getElementById("sentenceChip"),
      termChip: document.getElementById("termChip"),
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
      diffModified: document.getElementById("diffModified")
    };

    function escapeHtml(s) {
      const d = document.createElement("div");
      d.textContent = s == null ? "" : String(s);
      return d.innerHTML;
    }

    function key(versionId, mode) { return versionId + ":" + mode; }
    function getRules() { return data.rules[key(state.versionId, state.mode)] || []; }
    function setRules(rules) { data.rules[key(state.versionId, state.mode)] = rules; }
    function currentResourceSet() { return data.resourceSets.find(r => r.id === state.resourceSetId); }
    function versionsOfSet() { return data.versions[state.resourceSetId] || []; }
    function currentVersion() {
      return versionsOfSet().find(v => v.id === state.versionId);
    }
    function currentOnlineVersionId() { return currentResourceSet()?.currentVersionId; }
    function currentStagingVersionId() { return currentResourceSet()?.stagingVersionId; }
    function isEditable() { return state.viewMode === VIEW_MODE_EDIT; }
    function isDraftVersion(versionId) {
      const v = versionsOfSet().find(x => x.id === versionId);
      return !!v && v.status === "draft";
    }
    function versionLabel(versionId) {
      const v = versionsOfSet().find(x => x.id === versionId);
      if (!v) return "-";
      return "v" + v.versionNo + " (" + v.status + ")";
    }
    function resolveViewingVersionId() {
      const rs = currentResourceSet();
      if (state.viewMode === VIEW_MODE_EDIT) return rs.stagingVersionId;
      if (state.viewVersion !== VIEW_VERSION_ONLINE) return Number(state.viewVersion);
      return rs.currentVersionId;
    }

    function addAudit(message) {
      const now = new Date().toLocaleString();
      state.auditHistory.unshift(now + " | " + message);
      el.auditLog.innerHTML = state.auditHistory.slice(0, 30).map(i => escapeHtml(i)).join("<br>");
    }
    function addPublishLog(message) {
      const now = new Date().toLocaleString();
      state.publishHistory.unshift(now + " | " + message);
      el.publishLog.innerHTML = state.publishHistory.slice(0, 30).map(i => escapeHtml(i)).join("<br>");
    }

    function renderResourceSets() {
      el.resourceSetSelect.innerHTML = data.resourceSets.map(r =>
        "<option value='" + r.id + "'>" + r.name + "（scene=" + r.scene + ", env=" + r.env + "）</option>"
      ).join("");
      el.resourceSetSelect.value = String(state.resourceSetId);
    }

    function renderContextBar() {
      const rs = currentResourceSet();
      const onlineId = currentOnlineVersionId();
      const stagingId = currentStagingVersionId();
      const onlineV = versionsOfSet().find(v => v.id === onlineId);
      const stagingV = versionsOfSet().find(v => v.id === stagingId);

      // 模式 UI
      el.viewModeBtn.classList.toggle("active", state.viewMode === VIEW_MODE_VIEW);
      el.editModeBtn.classList.toggle("active", state.viewMode === VIEW_MODE_EDIT);
      el.viewVersionSelect.disabled = state.viewMode !== VIEW_MODE_VIEW;

      // 查看版本选择（一个下拉：线上 + 历史）
      const histories = versionsOfSet()
        .filter(v => v.status !== "draft") // published/archived
        .sort((a, b) => b.versionNo - a.versionNo);

      const onlineOption = "<option value='" + VIEW_VERSION_ONLINE + "'>线上当前版本（current_version_id）</option>";
      const historyOptions = histories.length
        ? histories.map(v => "<option value='" + v.id + "'>历史：v" + v.versionNo + " [" + v.status + "]</option>").join("")
        : "<option value='' disabled>— 无历史版本 —</option>";
      el.viewVersionSelect.innerHTML = onlineOption + historyOptions;

      // 校正 state.viewVersion
      if (state.viewVersion !== VIEW_VERSION_ONLINE) {
        const vid = Number(state.viewVersion);
        if (!histories.some(v => v.id === vid)) state.viewVersion = VIEW_VERSION_ONLINE;
      }
      el.viewVersionSelect.value = String(state.viewVersion);

      // 当前查看版本
      const viewingId = resolveViewingVersionId();
      el.viewingVersionText.textContent = versionLabel(viewingId);
      el.viewingHintText.textContent = (state.viewMode === VIEW_MODE_EDIT) ? "工作区可编辑" : "只读";

      // 顶部上下文文案
      el.topContext.textContent =
        "资源集：" + rs.name +
        " ｜ scene：" + rs.scene +
        " ｜ env：" + rs.env +
        " ｜ 线上：" + (onlineV ? ("v" + onlineV.versionNo) : "-") +
        " ｜ 工作区：" + (stagingV ? ("v" + stagingV.versionNo) : "-");

      // 只读态提示
      el.editabilityHint.textContent = isEditable() ? "工作区可编辑" : "只读查看";
    }

    function ensureStagingExists() {
      const rs = currentResourceSet();
      const versions = versionsOfSet();
      if (rs.stagingVersionId && isDraftVersion(rs.stagingVersionId)) return;
      const baseId = rs.currentVersionId || versions.find(v => v.status === "published")?.id;
      const baseV = versions.find(v => v.id === baseId);
      const nextNo = Math.max.apply(null, versions.map(v => v.versionNo)) + 1;
      const newId = Number(String(state.resourceSetId) + String(nextNo));
      versions.push({ id: newId, versionNo: nextNo, status: "draft" });
      data.rules[key(newId, MODE_SENTENCE)] = JSON.parse(JSON.stringify(data.rules[key(baseId, MODE_SENTENCE)] || []));
      data.rules[key(newId, MODE_TERM)] = JSON.parse(JSON.stringify(data.rules[key(baseId, MODE_TERM)] || []));
      rs.stagingVersionId = newId;
      addAudit("初始化工作区：从线上 v" + (baseV ? baseV.versionNo : baseId) + " 复制为工作区 v" + nextNo);
    }

    function renderComparePickerOptions() {
      // 对比入口：只允许在工作区视图下，基准默认是线上（current_version_id），也可选历史
      const onlineId = currentOnlineVersionId();
      const candidates = versionsOfSet().filter(v => v.status !== "draft"); // published/archived
      if (candidates.length === 0) {
        el.comparePickerSelect.innerHTML = "<option value=\"\">— 无线上/历史版本可对比 —</option>";
        return;
      }
      el.comparePickerSelect.innerHTML = candidates.map(v => {
        const prefix = v.id === onlineId ? "线上 " : "历史 ";
        return "<option value='" + v.id + "'>" + prefix + "v" + v.versionNo + " [" + v.status + "]</option>";
      }).join("");
      const defaultId = candidates.some(v => v.id === onlineId) ? onlineId : candidates[0].id;
      el.comparePickerSelect.value = String(defaultId);
    }

    function openComparePicker() {
      if (!isEditable()) return;
      renderComparePickerOptions();
      const has = !!el.comparePickerSelect.value;
      el.comparePickerOkBtn.disabled = !has;
      el.comparePickerMask.classList.add("show");
    }

    function closeComparePicker() {
      el.comparePickerMask.classList.remove("show");
    }

    function renderMode() {
      el.sentenceChip.classList.toggle("active", state.mode === MODE_SENTENCE);
      el.termChip.classList.toggle("active", state.mode === MODE_TERM);
    }

    function filteredRules() {
      const k = el.searchInput.value.trim().toLowerCase();
      if (!k) return getRules();
      return getRules().filter(r => (r.source || "").toLowerCase().includes(k) || (r.target || "").toLowerCase().includes(k));
    }

    function renderTable() {
      const rows = filteredRules();
      if (state.mode === MODE_SENTENCE) {
        el.tableHeader.innerHTML = "<th><input id='checkAll' type='checkbox'></th><th>#</th><th>source</th><th>target</th><th>matchType</th><th>priority</th><th>enabled</th><th>操作</th>";
      } else {
        el.tableHeader.innerHTML = "<th><input id='checkAll' type='checkbox'></th><th>#</th><th>source</th><th>target</th><th>priority</th><th>enabled</th><th>操作</th>";
      }
      const colspan = state.mode === MODE_SENTENCE ? 8 : 7;
      if (rows.length === 0) {
        el.tableBody.innerHTML = "<tr><td colspan='" + colspan + "' style='text-align:center;color:#5f6b7a;padding:16px;'>暂无规则</td></tr>";
      } else {
        el.tableBody.innerHTML = rows.map((r, idx) => {
          const matchCell = state.mode === MODE_SENTENCE
            ? "<td><select class='select js-match' data-id='" + r.id + "'>" +
              ["EXACT", "PREFIX", "CONTAINS"].map(m => "<option value='" + m + "'" + (r.matchType === m ? " selected" : "") + ">" + m + "</option>").join("") +
              "</select></td>"
            : "";
          return "<tr" + (r.selected ? " class='selected'" : "") + ">" +
            "<td><input type='checkbox' class='js-select' data-id='" + r.id + "'" + (r.selected ? " checked" : "") + "></td>" +
            "<td>" + (idx + 1) + "</td>" +
            "<td><input class='input js-source' data-id='" + r.id + "' value='" + escapeHtml(r.source) + "'" + (isEditable() ? "" : " disabled") + "></td>" +
            "<td><input class='input js-target' data-id='" + r.id + "' value='" + escapeHtml(r.target) + "'" + (isEditable() ? "" : " disabled") + "></td>" +
            matchCell +
            "<td><input type='number' class='input js-priority' data-id='" + r.id + "' value='" + Number(r.priority || 0) + "'" + (isEditable() ? "" : " disabled") + "></td>" +
            "<td><input type='checkbox' class='js-enabled' data-id='" + r.id + "'" + (r.enabled ? " checked" : "") + (isEditable() ? "" : " disabled") + "></td>" +
            "<td class='actions'><button class='btn js-del' data-id='" + r.id + "'" + (isEditable() ? "" : " disabled") + ">删除</button></td>" +
            "</tr>";
        }).join("");
      }

      const checkAll = document.getElementById("checkAll");
      if (checkAll) {
        checkAll.addEventListener("change", e => {
          const checked = e.target.checked;
          getRules().forEach(r => { r.selected = checked; });
          renderTable();
        });
      }

      bindRuleEvents();
      refreshButtons();
    }

    function bindRuleEvents() {
      el.tableBody.querySelectorAll(".js-select").forEach(i => i.addEventListener("change", e => {
        const r = getRules().find(x => x.id === Number(e.target.dataset.id));
        if (!r) return;
        r.selected = e.target.checked;
        refreshButtons();
      }));
      el.tableBody.querySelectorAll(".js-source").forEach(i => i.addEventListener("input", e => updateRule(Number(e.target.dataset.id), "source", e.target.value)));
      el.tableBody.querySelectorAll(".js-target").forEach(i => i.addEventListener("input", e => updateRule(Number(e.target.dataset.id), "target", e.target.value)));
      el.tableBody.querySelectorAll(".js-priority").forEach(i => i.addEventListener("input", e => updateRule(Number(e.target.dataset.id), "priority", Number(e.target.value || 0))));
      el.tableBody.querySelectorAll(".js-enabled").forEach(i => i.addEventListener("change", e => updateRule(Number(e.target.dataset.id), "enabled", e.target.checked)));
      el.tableBody.querySelectorAll(".js-del").forEach(i => i.addEventListener("click", e => {
        const id = Number(e.target.dataset.id);
        setRules(getRules().filter(r => r.id !== id));
        addAudit("删除规则 id=" + id + "（versionId=" + state.versionId + "）");
        renderTable();
      }));
      if (state.mode === MODE_SENTENCE) {
        el.tableBody.querySelectorAll(".js-match").forEach(i => i.addEventListener("change", e => updateRule(Number(e.target.dataset.id), "matchType", e.target.value)));
      }
    }

    function updateRule(id, key, val) {
      const r = getRules().find(x => x.id === id);
      if (!r) return;
      r[key] = val;
    }

    function refreshButtons() {
      const selectedCount = getRules().filter(r => r.selected).length;
      const canEdit = isEditable();
      el.addRuleBtn.disabled = !canEdit;
      el.batchEnableBtn.disabled = !canEdit || selectedCount === 0;
      el.batchDisableBtn.disabled = !canEdit || selectedCount === 0;
      el.batchDeleteBtn.disabled = !canEdit || selectedCount === 0;
      el.publishBtn.disabled = !canEdit;
      el.compareBtn.disabled = !canEdit;
      el.resetStagingBtn.disabled = !canEdit;
      el.validateBtn.disabled = !canEdit;
      // 回滚/reload 属于线上动作，查看模式也允许（原型模拟）
      el.rollbackBtn.disabled = false;
      el.reloadBtn.disabled = false;
    }

    function addRule() {
      if (!isEditable()) return;
      const base = { id: state.nextRuleId++, source: "", target: "", priority: 0, enabled: true, selected: false };
      if (state.mode === MODE_SENTENCE) base.matchType = "EXACT";
      getRules().push(base);
      addAudit("新增规则 id=" + base.id + "（versionId=" + state.versionId + "）");
      renderTable();
    }

    function applyBatch(action) {
      if (!isEditable()) return;
      getRules().forEach(r => {
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
      setRules(getRules().filter(r => !r.selected));
      addAudit("批量删除 " + (before - getRules().length) + " 条（versionId=" + state.versionId + "）");
      renderTable();
    }

    function validateRules() {
      const rules = getRules();
      const issues = [];
      const counter = new Map();
      rules.forEach((r, idx) => {
        const i = idx + 1;
        if (!r.source || !r.source.trim()) issues.push("第 " + i + " 行 source 为空");
        if (!r.target || !r.target.trim()) issues.push("第 " + i + " 行 target 为空");
        if (state.mode === MODE_SENTENCE && !["EXACT", "PREFIX", "CONTAINS"].includes(r.matchType)) {
          issues.push("第 " + i + " 行 matchType 非法");
        }
        const k = (r.source || "").trim();
        if (k) counter.set(k, (counter.get(k) || 0) + 1);
      });
      counter.forEach((v, k) => {
        if (v > 1) issues.push("source 重复：" + k + "（共 " + v + " 条）");
      });
      return issues;
    }

    function showValidateResult(issues) {
      const enabledCount = getRules().filter(r => r.enabled).length;
      const list = [];
      list.push("<li class='ok'>规则总数：" + getRules().length + "，启用：" + enabledCount + "</li>");
      if (issues.length === 0) {
        list.push("<li class='ok'>校验通过，可发布</li>");
      } else {
        list.push("<li class='warn-text'>校验失败，共 " + issues.length + " 项</li>");
        issues.slice(0, 10).forEach(i => list.push("<li class='err'>" + escapeHtml(i) + "</li>"));
      }
      el.validateList.innerHTML = list.join("");
    }

    function publish() {
      // 已由“确认发布”弹窗执行
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
      // 默认聚焦输入
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

      // 模拟 A+ 发布：staging(draft)->published，切 current_version_id，并生成新 staging(draft)
      const rs = currentResourceSet();
      const versions = versionsOfSet();
      const stagingId = rs.stagingVersionId;
      const stagingV = versions.find(v => v.id === stagingId);
      if (!stagingV || stagingV.status !== "draft") {
        alert("工作区版本异常，无法发布（模拟）");
        return;
      }

      // 历史 published -> archived（可选策略：保留多 published；此处保持原型“唯一 published”便于展示）
      versions.forEach(v => { if (v.status === "published") v.status = "archived"; });
      stagingV.status = "published";
      rs.currentVersionId = stagingId;
      addPublishLog("发布成功：resourceSet=" + state.resourceSetId + ", online=v" + stagingV.versionNo + ", mode=" + state.mode);
      addAudit("发布：current_version_id=" + stagingId + "，changeLog=" + changeLog);

      // 生成新的 staging
      const nextNo = Math.max.apply(null, versions.map(v => v.versionNo)) + 1;
      const newDraftId = Number(String(state.resourceSetId) + String(nextNo));
      versions.push({ id: newDraftId, versionNo: nextNo, status: "draft" });
      data.rules[key(newDraftId, MODE_SENTENCE)] = JSON.parse(JSON.stringify(data.rules[key(rs.currentVersionId, MODE_SENTENCE)] || []));
      data.rules[key(newDraftId, MODE_TERM)] = JSON.parse(JSON.stringify(data.rules[key(rs.currentVersionId, MODE_TERM)] || []));
      rs.stagingVersionId = newDraftId;

      // 发布后仍停留在工作区视图
      state.versionId = rs.stagingVersionId;
      el.publishChangeLogInput.value = "";
      renderAll();
    }

    function rollback() {
      openRollbackPicker();
    }

    function reload() {
      addPublishLog("已触发 Reload（模拟）：resourceSet=" + state.resourceSetId + ", versionId=" + state.versionId);
      alert("已触发实例 Reload（模拟）");
    }

    function resetStagingToOnline(silent) {
      const rs = currentResourceSet();
      const versions = versionsOfSet();
      const onlineId = rs.currentVersionId;
      const onlineV = versions.find(v => v.id === onlineId);
      if (!onlineId || !onlineV) {
        alert("线上版本不存在（模拟）");
        return;
      }
      // 删除旧 staging（仅原型模拟：直接标记 archived）
      const oldStagingId = rs.stagingVersionId;
      const oldV = versions.find(v => v.id === oldStagingId);
      if (oldV && oldV.status === "draft") oldV.status = "archived";

      const nextNo = Math.max.apply(null, versions.map(v => v.versionNo)) + 1;
      const newId = Number(String(state.resourceSetId) + String(nextNo));
      versions.push({ id: newId, versionNo: nextNo, status: "draft" });
      data.rules[key(newId, MODE_SENTENCE)] = JSON.parse(JSON.stringify(data.rules[key(onlineId, MODE_SENTENCE)] || []));
      data.rules[key(newId, MODE_TERM)] = JSON.parse(JSON.stringify(data.rules[key(onlineId, MODE_TERM)] || []));
      rs.stagingVersionId = newId;
      state.versionId = newId;
      if (!silent) addAudit("重置工作区=线上 v" + onlineV.versionNo + " -> 新工作区 v" + nextNo);
    }

    function renderRollbackPickerOptions() {
      const onlineId = currentOnlineVersionId();
      const candidates = versionsOfSet().filter(v => v.status !== "draft" && v.id !== onlineId);
      if (candidates.length === 0) {
        el.rollbackPickerSelect.innerHTML = "<option value=\"\">— 无可回滚版本 —</option>";
        return;
      }
      // 默认选择最近的 archived/published（按 versionNo 倒序）
      const sorted = [...candidates].sort((a, b) => b.versionNo - a.versionNo);
      el.rollbackPickerSelect.innerHTML = sorted.map(v =>
        "<option value='" + v.id + "'>v" + v.versionNo + " [" + v.status + "]</option>"
      ).join("");
      el.rollbackPickerSelect.value = String(sorted[0].id);
    }

    function openRollbackPicker() {
      renderRollbackPickerOptions();
      const has = !!el.rollbackPickerSelect.value;
      el.rollbackPickerOkBtn.disabled = !has;
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
      const target = versions.find(v => v.id === toVersionId);
      if (!target || target.status === "draft") {
        alert("版本选择非法");
        return;
      }
      closeRollbackPicker();

      // 模拟回滚：切线上 published 指针
      versions.forEach(v => { if (v.status === "published") v.status = "archived"; });
      target.status = "published";
      currentResourceSet().currentVersionId = target.id;
      addPublishLog("回滚成功：切换到 v" + target.versionNo);
      addAudit("回滚至 versionId=" + target.id + "，并同步重置工作区=线上");
      // 默认同步工作区：用目标线上覆盖/重建 staging
      resetStagingToOnline(true);
      renderAll();
    }

    function compareVersion(baseId) {
      const base = data.rules[key(baseId, state.mode)] || [];
      const cur = getRules();
      const baseMap = new Map(base.map(r => [(r.source || "").trim(), r]));
      const curMap = new Map(cur.map(r => [(r.source || "").trim(), r]));

      const added = [];
      const deleted = [];
      const modified = [];

      curMap.forEach((v, k) => {
        if (!k) return;
        if (!baseMap.has(k)) {
          added.push(v);
          return;
        }
        const b = baseMap.get(k);
        const same = state.mode === MODE_SENTENCE
          ? (b.target === v.target && b.matchType === v.matchType && Number(b.priority || 0) === Number(v.priority || 0) && !!b.enabled === !!v.enabled)
          : (b.target === v.target && Number(b.priority || 0) === Number(v.priority || 0) && !!b.enabled === !!v.enabled);
        if (!same) modified.push({ source: k, base: b, current: v });
      });
      baseMap.forEach((v, k) => {
        if (!k) return;
        if (!curMap.has(k)) deleted.push(v);
      });

      function renderRuleLine(rule) {
        if (state.mode === MODE_SENTENCE) {
          return "source=" + (rule.source || "-") + " | target=" + (rule.target || "-") + " | match=" + (rule.matchType || "EXACT") + " | p=" + Number(rule.priority || 0) + " | enabled=" + (!!rule.enabled);
        }
        return "source=" + (rule.source || "-") + " | target=" + (rule.target || "-") + " | p=" + Number(rule.priority || 0) + " | enabled=" + (!!rule.enabled);
      }

      el.diffAdded.innerHTML = added.length
        ? added.map(r => "<div class='diff-line diff-add'>+ " + escapeHtml(renderRuleLine(r)) + "</div>").join("")
        : "<div class='hint'>无新增</div>";

      el.diffDeleted.innerHTML = deleted.length
        ? deleted.map(r => "<div class='diff-line diff-del'>- " + escapeHtml(renderRuleLine(r)) + "</div>").join("")
        : "<div class='hint'>无删除</div>";

      el.diffModified.innerHTML = modified.length
        ? modified.map(m =>
          "<div class='diff-line diff-mod'>~ source=" + escapeHtml(m.source) + "</div>" +
          "<div class='diff-line'>  base: " + escapeHtml(renderRuleLine(m.base)) + "</div>" +
          "<div class='diff-line'>  cur : " + escapeHtml(renderRuleLine(m.current)) + "</div>"
        ).join("")
        : "<div class='hint'>无变更</div>";

      const baseVersion = (data.versions[state.resourceSetId] || []).find(v => v.id === baseId);
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

    function preview() {
      const query = el.previewInput.value;
      if (!query) {
        el.previewOutput.textContent = "请先输入 query";
        return;
      }
      let rewritten = query;
      const hits = [];
      const active = getRules().filter(r => r.enabled);
      if (state.mode === MODE_SENTENCE) {
        const sorted = [...active].sort((a, b) => Number(b.priority || 0) - Number(a.priority || 0));
        for (const r of sorted) {
          if (r.matchType === "EXACT" && rewritten === r.source) { rewritten = r.target; hits.push(r); break; }
          if (r.matchType === "PREFIX" && rewritten.startsWith(r.source)) { rewritten = r.target; hits.push(r); break; }
          if (r.matchType === "CONTAINS" && rewritten.includes(r.source)) { rewritten = r.target; hits.push(r); break; }
        }
      } else {
        const sorted = [...active].sort((a, b) => (b.source || "").length - (a.source || "").length);
        sorted.forEach(r => {
          if (!r.source) return;
          if (rewritten.includes(r.source)) {
            rewritten = rewritten.split(r.source).join(r.target || "");
            hits.push(r);
          }
        });
      }
      el.previewOutput.innerHTML = "输入：<code>" + escapeHtml(query) + "</code><br>命中：" + hits.length + " 条<br>输出：<code>" + escapeHtml(rewritten) + "</code>";
    }

    function renderAll() {
      ensureStagingExists();
      const rs = currentResourceSet();
      state.versionId = resolveViewingVersionId();
      renderResourceSets();
      renderContextBar();
      renderMode();
      renderTable();
      showValidateResult(validateRules());
      refreshButtons();
      if (!el.publishLog.innerHTML) addPublishLog("等待发布操作...");
      if (!el.auditLog.innerHTML) addAudit("页面初始化完成");
    }

    el.resourceSetSelect.addEventListener("change", e => {
      state.resourceSetId = Number(e.target.value);
      state.viewMode = VIEW_MODE_VIEW;
      state.viewVersion = VIEW_VERSION_ONLINE;
      addAudit("切换资源集 resourceSetId=" + state.resourceSetId);
      renderAll();
    });
    el.sentenceChip.addEventListener("click", () => { state.mode = MODE_SENTENCE; renderAll(); });
    el.termChip.addEventListener("click", () => { state.mode = MODE_TERM; renderAll(); });
    el.searchInput.addEventListener("input", renderTable);

    el.viewModeBtn.addEventListener("click", () => {
      state.viewMode = VIEW_MODE_VIEW;
      state.viewVersion = VIEW_VERSION_ONLINE;
      addAudit("切换模式：查看（线上）");
      renderAll();
    });
    el.editModeBtn.addEventListener("click", () => {
      state.viewMode = VIEW_MODE_EDIT;
      addAudit("切换模式：编辑（进入工作区）");
      renderAll();
    });
    el.viewVersionSelect.addEventListener("change", e => {
      state.viewVersion = e.target.value;
      addAudit("切换查看版本：" + state.viewVersion);
      renderAll();
    });

    el.addRuleBtn.addEventListener("click", addRule);
    el.batchEnableBtn.addEventListener("click", () => applyBatch("enable"));
    el.batchDisableBtn.addEventListener("click", () => applyBatch("disable"));
    el.batchDeleteBtn.addEventListener("click", deleteSelected);
    el.validateBtn.addEventListener("click", () => {
      const issues = validateRules();
      showValidateResult(issues);
      alert(issues.length ? ("校验失败：\n- " + issues.join("\n- ")) : "校验通过");
    });
    el.publishBtn.addEventListener("click", publish);
    el.rollbackBtn.addEventListener("click", rollback);
    el.reloadBtn.addEventListener("click", reload);
    el.compareBtn.addEventListener("click", openComparePicker);
    el.resetStagingBtn.addEventListener("click", () => {
      if (!isEditable()) return;
      const ok = confirm("将重置工作区为线上版本，未发布变更将丢失。继续？");
      if (!ok) return;
      resetStagingToOnline(false);
      renderAll();
    });
    el.comparePickerOkBtn.addEventListener("click", confirmCompare);
    el.comparePickerCancelBtn.addEventListener("click", closeComparePicker);
    el.comparePickerCloseBtn.addEventListener("click", closeComparePicker);
    el.comparePickerMask.addEventListener("click", e => {
      if (e.target === el.comparePickerMask) closeComparePicker();
    });
    el.previewBtn.addEventListener("click", preview);
    el.compareModalCloseBtn.addEventListener("click", () => el.compareModalMask.classList.remove("show"));
    el.compareModalMask.addEventListener("click", e => {
      if (e.target === el.compareModalMask) el.compareModalMask.classList.remove("show");
    });

    el.rollbackPickerOkBtn.addEventListener("click", confirmRollback);
    el.rollbackPickerCancelBtn.addEventListener("click", closeRollbackPicker);
    el.rollbackPickerCloseBtn.addEventListener("click", closeRollbackPicker);
    el.rollbackPickerMask.addEventListener("click", e => {
      if (e.target === el.rollbackPickerMask) closeRollbackPicker();
    });

    el.publishConfirmOkBtn.addEventListener("click", confirmPublish);
    el.publishConfirmCancelBtn.addEventListener("click", closePublishConfirm);
    el.publishConfirmCloseBtn.addEventListener("click", closePublishConfirm);
    el.publishConfirmMask.addEventListener("click", e => {
      if (e.target === el.publishConfirmMask) closePublishConfirm();
    });

    renderAll();
};

