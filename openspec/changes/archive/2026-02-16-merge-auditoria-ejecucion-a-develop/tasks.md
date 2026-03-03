## 1. Sync de ramas y alcance de integracion

- [x] 1.1 Sincronizar `feature/auditoria-fase-1-ejecucion` con remoto antes de editar (incluyendo pushes de Copilot).
- [x] 1.2 Revisar delta contra `develop` y delimitar alcance exacto del PR de integracion.
- [x] 1.3 Confirmar que no existan cambios locales sin versionar antes de preparar commits finales.

## 2. Alineacion documental con formato de referencia

- [x] 2.1 Revisar `docs/auditoria/` contra el formato de ejemplo usado en `docs/refactor/`.
- [x] 2.2 Ajustar estructura de documentos de evidencia para incluir secciones obligatorias (resumen, commits, archivos, evidencia funcional, estado).
- [x] 2.3 Actualizar `AUDITORIA.md` para mantener enlaces y estado de mitigacion coherentes con la evidencia.

## 3. Verificacion tecnica previa a merge

- [x] 3.1 Ejecutar pruebas backend relevantes (`mvn -pl order-service,kitchen-worker test`) y registrar resultado.
- [x] 3.2 Validar que OpenSpec del cambio este completo y consistente con los artefactos creados.
- [x] 3.3 Verificar que el diff final no introduzca regresiones en configuracion o contratos de eventos ya mitigados.

## 4. Integracion a develop

- [x] 4.1 Commit/push de ajustes finales en `feature/auditoria-fase-1-ejecucion`.
- [x] 4.2 Crear PR con `base: develop` y `compare: feature/auditoria-fase-1-ejecucion` incluyendo checklist de validacion.
- [x] 4.3 Definir y documentar plan de rollback (revert de merge commit) y pasos post-merge para actualizar local `develop`.
