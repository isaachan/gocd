/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.materials.dependency;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialInstance;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.materials.dependency.ExternalDependencyMaterialInstance;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.json.JsonMap;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.lang.String.format;

public class ExternalDependencyMaterial extends AbstractMaterial {
    private static final Logger LOGGER = Logger.getLogger(ExternalDependencyMaterial.class);
    public static final String TYPE = "DependencyMaterial";

    private CaseInsensitiveString pipelineName = new CaseInsensitiveString("Unknown");
    private CaseInsensitiveString stageName = new CaseInsensitiveString("Unknown");
    private CaseInsensitiveString serverAlias = new CaseInsensitiveString("Unknown");

    public ExternalDependencyMaterial() {
        super("DependencyMaterial");
    }

    public ExternalDependencyMaterial(final CaseInsensitiveString serverAlias, final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        this(null, serverAlias, pipelineName, stageName);
    }

    public ExternalDependencyMaterial(final CaseInsensitiveString name, final CaseInsensitiveString serverAlias, final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        this();
        bombIfNull(serverAlias, "null serverAlias");
        bombIfNull(pipelineName, "null pipelineName");
        bombIfNull(stageName, "null stageName");
        this.serverAlias = serverAlias;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.name = name;
    }

    public ExternalDependencyMaterial(ExternalDependencyMaterialConfig config) {
        this(config.getName(), config.getServerAlias(), config.getPipelineName(), config.getStageName());
    }

    @Override
    public MaterialConfig config() {
        return new ExternalDependencyMaterialConfig(name, serverAlias, pipelineName, stageName);
    }

    @Override public CaseInsensitiveString getName() {
        return super.getName() == null ? pipelineName : super.getName();
    }

    public String getUserName() {
        return "cruise";
    }

    //Unused (legacy) methods

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision, File baseDir, final SubprocessExecutionContext execCtx) {

    }

    @Override protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        //Dependency materials are already unique within a pipeline
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        return null;
    } //OLD

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        return null;
    } //NEW

    public Revision oldestRevision(Modifications modifications) {
        if(modifications.size() > 1){
            LOGGER.warn(String.format("Dependency material %s has multiple modifications", this.getDisplayName()));
        }
        Modification oldestModification = modifications.get(modifications.size() -1);
        String revision = oldestModification.getRevision();
        return DependencyMaterialRevision.create(revision, oldestModification.getPipelineLabel());
    }

    @Override
    public String getLongDescription() {
        return getDescription();
    }

    public void toJson(JsonMap json, Revision revision) {
        json.put("folder", getFolder());
        json.put("scmType", "Dependency");
        json.put("location", serverAlias + "/" + pipelineName + "/" + stageName);
        json.put("action", "Completed");
        if (!CaseInsensitiveString.isBlank(getName())) {
            json.put("materialName", CaseInsensitiveString.str(getName()));
        }
    }

    public boolean matches(String name, String regex) {
        return false;
    }

    public void emailContent(StringBuilder content, Modification modification) {
        content.append("ExternalDependency: " + serverAlias + "/" + pipelineName + "/" + stageName).append('\n').append(
                String.format("revision: %s, completed on %s", modification.getRevision(),
                        modification.getModifiedTime()));
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        throw new UnsupportedOperationException("findModificationsSince is not supported on " + this);
    }

    public MaterialInstance createMaterialInstance() {
        return new ExternalDependencyMaterialInstance(CaseInsensitiveString.str(pipelineName), CaseInsensitiveString.str(stageName), UUID.randomUUID().toString(), CaseInsensitiveString.str(serverAlias));
    }

    public String getDescription() {
        return CaseInsensitiveString.str(pipelineName);
    }

    public String getTypeForDisplay() {
        return "Pipeline";
    }

    public void populateEnvironmentContext(EnvironmentVariableContext context, MaterialRevision materialRevision, File workingDir) {
        DependencyMaterialRevision revision = (DependencyMaterialRevision) materialRevision.getRevision();
        context.setPropertyWithEscape(format("GO_DEPENDENCY_LABEL_%s", getName()), revision.getPipelineLabel());
        context.setPropertyWithEscape(format("GO_DEPENDENCY_LOCATOR_%s", getName()), revision.getRevision());
    }

    public boolean isAutoUpdate() {
        return true;
    }

    public final MatchedRevision createMatchedRevision(Modification modification, String searchString) {
        return new MatchedRevision(searchString, modification.getRevision(), modification.getModifiedTime(), modification.getPipelineLabel());
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put("serverAlias", CaseInsensitiveString.str(serverAlias));
        parameters.put("pipelineName", CaseInsensitiveString.str(pipelineName));
        parameters.put("stageName", CaseInsensitiveString.str(stageName));
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    public CaseInsensitiveString getPipelineName() {
        return pipelineName;
    }

    public CaseInsensitiveString getStageName() {
        return stageName;
    }

    public String getFolder() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalDependencyMaterial that = (ExternalDependencyMaterial) o;
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if(serverAlias !=null ? !serverAlias.equals(that.serverAlias) : that.serverAlias != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (serverAlias != null ? serverAlias.hashCode() : 0);
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "ExternalDependencyMaterial{" +
                "serverAlias='" + serverAlias + '\'' +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                '}';
    }

    public String getDisplayName() {
        return CaseInsensitiveString.str(getName());
    }

    public String getUriForDisplay() {
        return String.format("%s %s / %s", serverAlias, pipelineName, stageName);
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<String, Object>();
        materialMap.put("type", "pipeline");
        Map<String, Object> configurationMap = new HashMap<String, Object>();
        configurationMap.put("server-name", serverAlias.toString());
        configurationMap.put("pipeline-name", pipelineName.toString());
        configurationMap.put("stage-name", stageName.toString());
        materialMap.put("pipeline-configuration", configurationMap);
        return materialMap;
    }

    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig){
        List<FetchTask> fetchTasks = pipelineConfig.getFetchTasks();
        for (FetchTask fetchTask : fetchTasks) {
            if(pipelineName.equals(fetchTask.getDirectParentInAncestorPath()))
                return true;
        }
        return false;
    }

    public Class getInstanceType() {
        return ExternalDependencyMaterialInstance.class;
    }
}
