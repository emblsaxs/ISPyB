<%--------------------------------------------------------------------------------------------------
This file is part of ISPyB.

ISPyB is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ISPyB is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with ISPyB.  If not, see <http://www.gnu.org/licenses/>.

Contributors : S. Delageniere, R. Leal, L. Launer, K. Levik, S. Veyrier, P. Brenchereau, M. Bodin, A. De Maria Antolinos
--------------------------------------------------------------------------------------------------%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://struts.application-servers.com/layout" prefix="layout" %>

<% 
    String targetDataCollectionGroup = request.getContextPath() + "/user/viewDataCollection.do?reqCode=displayForDataCollectionGroup";
%>

<%-- Selected  DataCollectionGroup --%>
<logic:present name="breadCrumbsForm" property="selectedDataCollectionGroup" scope="session">
	<bean:define name="breadCrumbsForm" property="selectedDataCollectionGroup" 	scope="session" id="selectedDataCollectionGroup" 	type="ispyb.server.mx.vos.collections.DataCollectionGroup3VO"/>
	<%-- Selected DataCollectionGroup Info --%>
	<layout:column styleClass="PANEL_BREAD_CRUMBS">
		<layout:panel key="Selected DataCollectionGroup" align="left" styleClass="PANEL_BREAD_CRUMBS_ITEM">
			<layout:text key="Start Time" 	name="selectedDataCollectionGroup" 	property="startTime" styleClass="PANEL_BREAD_CRUMBS_FIELD" mode="I,I,I" type="dateTime" />
			<layout:text key="Experiment Type" 	name="selectedDataCollectionGroup" 	property="experimentType" styleClass="PANEL_BREAD_CRUMBS_FIELD" mode="I,I,I" />
			<tr><th class="PANEL_BREAD_CRUMBS_FIELD" valign="top"></th><td class="PANEL_BREAD_CRUMBS_FIELD" valign="top" style="">
				<html:link href="<%=targetDataCollectionGroup%>" paramId="dataCollectionGroupId" paramName="selectedDataCollectionGroup" paramProperty="dataCollectionGroupId" styleClass="PANEL_BREAD_CRUMBS_FIELD">
					Back to this data collection group
				</html:link>
			</td></tr>
			
            
		</layout:panel>
	</layout:column>
</logic:present>
