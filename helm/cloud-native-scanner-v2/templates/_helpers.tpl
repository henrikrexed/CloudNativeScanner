{{/*
Common labels
*/}}
{{- define "scanner.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/part-of: cloud-native-scanner
{{- end }}

{{/*
Pipeline-service selector labels
*/}}
{{- define "pipeline.selectorLabels" -}}
app.kubernetes.io/name: pipeline-service
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
WebUI selector labels
*/}}
{{- define "webui.selectorLabels" -}}
app.kubernetes.io/name: webui
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Image tag helper — defaults to .Chart.AppVersion
*/}}
{{- define "pipeline.imageTag" -}}
{{- .Values.pipelineService.image.tag | default .Chart.AppVersion -}}
{{- end }}
{{- define "webui.imageTag" -}}
{{- .Values.webui.image.tag | default .Chart.AppVersion -}}
{{- end }}

{{/*
Database host — uses subchart service if postgresql.enabled
*/}}
{{- define "scanner.dbHost" -}}
{{- if .Values.postgresql.enabled -}}
{{ .Release.Name }}-postgresql
{{- else -}}
{{ .Values.externalDatabase.host }}
{{- end -}}
{{- end }}

{{- define "scanner.dbPort" -}}
{{- if .Values.postgresql.enabled -}}
5432
{{- else -}}
{{ .Values.externalDatabase.port | default 5432 }}
{{- end -}}
{{- end }}

{{- define "scanner.dbName" -}}
{{- if .Values.postgresql.enabled -}}
{{ .Values.postgresql.auth.database }}
{{- else -}}
{{ .Values.externalDatabase.database }}
{{- end -}}
{{- end }}

{{- define "scanner.dbUser" -}}
{{- if .Values.postgresql.enabled -}}
{{ .Values.postgresql.auth.username }}
{{- else -}}
{{ .Values.externalDatabase.username }}
{{- end -}}
{{- end }}

{{/*
Secret name for DB credentials
*/}}
{{- define "scanner.dbSecretName" -}}
{{- if .Values.postgresql.enabled -}}
  {{- if .Values.postgresql.auth.existingSecret -}}
  {{ .Values.postgresql.auth.existingSecret }}
  {{- else -}}
  {{ .Release.Name }}-db-credentials
  {{- end -}}
{{- else -}}
  {{- if .Values.externalDatabase.existingSecret -}}
  {{ .Values.externalDatabase.existingSecret }}
  {{- else -}}
  {{ .Release.Name }}-db-credentials
  {{- end -}}
{{- end -}}
{{- end }}

{{/*
Secret name for LLM API key
*/}}
{{- define "scanner.llmSecretName" -}}
{{- if .Values.llm.existingSecret -}}
{{ .Values.llm.existingSecret }}
{{- else -}}
{{ .Release.Name }}-llm-credentials
{{- end -}}
{{- end }}
