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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.DependencyFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@ConfigTag(value = "external_pipeline", label = "ExternalPipeline")
public class ExternalDependencyMaterialConfig extends AbstractMaterialConfig implements ParamsAttributeAware {
    public static final String SERVER_ALIAS = "serverAlias";
    public static final String PIPELINE_NAME = "pipelineName";
    public static final String STAGE_NAME = "stageName";
    public static final String SERVER_PIPELINE_STAGE_NAME = "serverPipelineStageName";
    public static final String TYPE = "ExternalDependencyMaterial";
    private static final Pattern PIPELINE_STAGE_COMBINATION_PATTERN = Pattern.compile("^(.+) (\\[.+\\])$");

    @ConfigAttribute(value = "serverAlias")
    private CaseInsensitiveString serverAlias = new CaseInsensitiveString("Unknown");

    @ConfigAttribute(value = "pipelineName")
    private CaseInsensitiveString pipelineName = new CaseInsensitiveString("Unknown");

    @ConfigAttribute(value = "stageName")
    private CaseInsensitiveString stageName = new CaseInsensitiveString("Unknown");

    private String pipelineStageName;


    public ExternalDependencyMaterialConfig() {
        super(TYPE);
    }



    public ExternalDependencyMaterialConfig(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, final CaseInsensitiveString serverAlias) {
        this(null, pipelineName, stageName, serverAlias);
    }

    public ExternalDependencyMaterialConfig(final CaseInsensitiveString name, final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, final CaseInsensitiveString serverAlias) {
        super(TYPE, name, new ConfigErrors());
        bombIfNull(serverAlias, "null serverAlias");
        bombIfNull(pipelineName, "null pipelineName");
        bombIfNull(stageName, "null stageName");
        this.serverAlias = serverAlias;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    @Override
    public CaseInsensitiveString getName() {
        return super.getName() == null ? pipelineName : super.getName();
    }

    public String getUserName() {
        return "cruise";
    }

    @Override
    public String getLongDescription() {
        return getDescription();
    }

    @Override
    public Filter filter() {
        return new DependencyFilter();
    }

    @Override
    public boolean matches(String name, String regex) {
        return false;
    }

    @Override
    public String getDescription() {
        return CaseInsensitiveString.str(pipelineName);
    }

    @Override
    public String getTypeForDisplay() {
        return "Pipeline";
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
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

    @Override
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
        ExternalDependencyMaterialConfig that = (ExternalDependencyMaterialConfig) o;
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if(serverAlias !=null? !serverAlias.equals(that.serverAlias) : that.serverAlias != null) {
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
        return "ExternalDependencyMaterialConfig{" +
                "serverAlias=" + serverAlias + '\'' +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                '}';
    }

    @Override
    public String getDisplayName() {
        return CaseInsensitiveString.str(getName());
    }

    @Override
    protected void validateConcreteMaterial(ValidationContext validationContext) {
        //todo: add validation for external dependency
    }

    @Override
    public String getUriForDisplay() {
        return String.format("%s / %s / %s", serverAlias, pipelineName, stageName);
    }

    public void validateUniqueness(Set<CaseInsensitiveString> dependencies) {
        CaseInsensitiveString upstreamPipelineName = pipelineName;
        if (dependencies.contains(serverAlias) && dependencies.contains(upstreamPipelineName)) {
            String message = (String.format("A pipeline can depend on each upstream pipeline only once. Remove one of the occurrences of '%s' from the current pipeline dependencies.", upstreamPipelineName));
            errors.add(SERVER_PIPELINE_STAGE_NAME, message);
        }
        dependencies.add(pipelineName);
    }

    @Override
    //what's the meaning of PIPELINE_STAGE_COMBINATION_PATTERN
    public void setConfigAttributes(Object attributes) {
        resetCachedIdentityAttributes();
        if (attributes == null) {
            return;
        }
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(MATERIAL_NAME)) {
            name = new CaseInsensitiveString((String) attributesMap.get(MATERIAL_NAME));
            if (CaseInsensitiveString.isBlank(name)) {
                name = null;
            }
        }
        if (attributesMap.containsKey(SERVER_PIPELINE_STAGE_NAME)) {
            pipelineStageName = (String) attributesMap.get(SERVER_PIPELINE_STAGE_NAME);
            Matcher matcher = PIPELINE_STAGE_COMBINATION_PATTERN.matcher(pipelineStageName);
            if(matcher.matches()){
                serverAlias = new CaseInsensitiveString(matcher.group(1));
                pipelineName = new CaseInsensitiveString(matcher.group(2));
                String stageNameWithBrackets = matcher.group(3);
                stageName = new CaseInsensitiveString(stageNameWithBrackets.replace("[","").replace("]",""));
            }
            else {
               errors.add(SERVER_PIPELINE_STAGE_NAME, String.format("'%s' should conform to the pattern 'pipeline [stage]'",pipelineStageName));
            }
        }
    }

    public String getPipelineStageName() {
        if (pipelineStageName != null) {
            return pipelineStageName;
        }
        if (CaseInsensitiveString.isBlank(pipelineName) || CaseInsensitiveString.isBlank(stageName)) {
            return null;
        }
        return String.format("%s [%s]", pipelineName, stageName);
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig){
        List<FetchTask> fetchTasks = pipelineConfig.getFetchTasks();
        for (FetchTask fetchTask : fetchTasks) {
            if(pipelineName.equals(fetchTask.getDirectParentInAncestorPath()))
                return true;
        }
        return false;
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        // Dependency materials are already unique within a pipeline
    }

    public CaseInsensitiveString getServerAlias() {
        return serverAlias;
    }
}