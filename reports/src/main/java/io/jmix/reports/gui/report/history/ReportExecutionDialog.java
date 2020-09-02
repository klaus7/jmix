/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.reports.gui.report.history;

import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import io.jmix.core.entity.BaseUser;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportGroup;
import io.jmix.reports.gui.ReportGuiManager;
import io.jmix.ui.WindowParam;
import io.jmix.ui.component.Button;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UiController("report$ReportExecution.dialog")
@UiDescriptor("report-execution-dialog.xml")
@LookupComponent("reportsTable")
@LoadDataBeforeShow
public class ReportExecutionDialog extends StandardLookup<Report> {

    public static final String META_CLASS_PARAMETER = "metaClass";
    public static final String SCREEN_PARAMETER = "screen";

    @Autowired
    protected ReportGuiManager reportGuiManager;
    @Autowired
    protected UserSessionSource userSessionSource;

    @Autowired
    protected CollectionContainer<Report> reportsDc;
    @Autowired
    protected Table<Report> reportsTable;

    @Autowired
    protected Button applyFilterBtn;
    @Autowired
    protected TextField<String> filterName;
    @Autowired
    protected TextField<String> filterCode;
    @Autowired
    protected LookupField<ReportGroup> filterGroup;
    @Autowired
    protected DateField<Date> filterUpdatedDate;

    @WindowParam(name = META_CLASS_PARAMETER)
    protected MetaClass metaClassParameter;
    @WindowParam(name = SCREEN_PARAMETER)
    protected String screenParameter;

    @Install(to = "reportsDl", target = Target.DATA_LOADER)
    protected List<Report> reportsDlLoadDelegate(LoadContext<Report> loadContext) {
        BaseUser sessionUser = userSessionSource.getUserSession().getUser();
        return reportGuiManager.getAvailableReports(screenParameter, sessionUser, metaClassParameter);
    }

    @Subscribe("clearFilterBtn")
    public void onClearFilterBtnClick(Button.ClickEvent event) {
        filterName.setValue(null);
        filterCode.setValue(null);
        filterUpdatedDate.setValue(null);
        filterGroup.setValue(null);
        filterReports();
    }

    @Subscribe("applyFilterBtn")
    public void onApplyFilterBtnClick(Button.ClickEvent event) {
        filterReports();
    }

    protected void filterReports() {
        BaseUser sessionUser = userSessionSource.getUserSession().getUser();
        List<Report> reports = reportGuiManager.getAvailableReports(screenParameter, sessionUser, metaClassParameter)
                .stream()
                .filter(this::filterReport)
                .collect(Collectors.toList());

        reportsDc.setItems(reports);

        Table.SortInfo sortInfo = reportsTable.getSortInfo();
        if (sortInfo != null) {
            Table.SortDirection direction = sortInfo.getAscending() ? Table.SortDirection.ASCENDING : Table.SortDirection.DESCENDING;
            reportsTable.sort(sortInfo.getPropertyId().toString(), direction);
        }
    }

    protected boolean filterReport(Report report) {
        String filterNameValue = StringUtils.lowerCase(filterName.getValue());
        String filterCodeValue = StringUtils.lowerCase(filterCode.getValue());
        ReportGroup groupFilterValue = filterGroup.getValue();
        Date dateFilterValue = filterUpdatedDate.getValue();

        if (filterNameValue != null
                && !report.getName().toLowerCase().contains(filterNameValue)) {
            return false;
        }

        if (filterCodeValue != null) {
            if (report.getCode() == null
                    || (report.getCode() != null
                    && !report.getCode().toLowerCase().contains(filterCodeValue))) {
                return false;
            }
        }

        if (groupFilterValue != null && !Objects.equals(report.getGroup(), groupFilterValue)) {
            return false;
        }

        if (dateFilterValue != null
                && report.getUpdateTs() != null
                && !report.getUpdateTs().after(dateFilterValue)) {
            return false;
        }

        return true;
    }
}
