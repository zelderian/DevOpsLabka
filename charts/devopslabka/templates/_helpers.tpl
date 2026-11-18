{{- define "devopslabka.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "devopslabka.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "devopslabka.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "devopslabka.selectorLabels" -}}
app.kubernetes.io/name: {{ include "devopslabka.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "devopslabka.labels" -}}
helm.sh/chart: {{ include "devopslabka.chart" . }}
{{ include "devopslabka.selectorLabels" . }}
app.kubernetes.io/version: {{ include "devopslabka.imageTag" . | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "devopslabka.imageTag" -}}
{{- .Values.image.tag | default .Chart.AppVersion }}
{{- end }}

{{- define "devopslabka.secretName" -}}
{{- .Values.existingSecret | default (printf "%s-secret" (include "devopslabka.fullname" .)) }}
{{- end }}
