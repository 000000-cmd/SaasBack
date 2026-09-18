# Probar liquidaciones con datos de prueba

El saldo por cobrar (`Balance = AmountAccrued - AmountPaid`) hoy es 0 para todos:
el `AmountAccrued` lo llenará el módulo de citas/servicios, que aún no existe.
Este script simula ese devengado para dejar **liquidaciones pendientes**.

## Cómo se corre

Con el stack levantado (`docker compose up -d`), un solo comando:

```bash
docker compose exec -T mysql mysql -uroot -prootpassword saas_db < scripts/seed-liquidaciones.sql
```

Eso es todo. Al terminar imprime una tabla con cada negocio, **el usuario del
dueño con el que debes entrar** y el total por liquidar.

## Qué hace

1. Pone un devengado aleatorio (200.000–1.500.000 COP) a cada empleado con fila
   de saldo, dejando el saldo pendiente.
2. **Inserta el evento en `outbox_event`** (`Status='PENDING'`). El relay de
   finance lo despacha a Kafka y search-service lo proyecta a Elasticsearch, que
   es de donde lee la pantalla. Sin este paso la pantalla no vería nada, porque
   el saldo en MySQL no basta.
3. Muestra el resumen.

Es idempotente: reejecutar solo cambia los montos.

### Ajustes

Editables al inicio del `.sql`:

```sql
SET @min_cop := 200000;    -- devengado mínimo
SET @max_cop := 1500000;   -- devengado máximo
SET @business := NULL;     -- pon un businessId para limitar a ese negocio
```

## Probar

Entra a **/tenant/liquidaciones** con el usuario que indicó el script:

- La lista muestra el saldo pendiente de cada empleado.
- **Ver detalles** abre el drawer con la información disponible.
- **Liquidar** abre el drawer para confirmar; al confirmar corre la animación de
  abono y el saldo baja.
- El **historial** se llena con las liquidaciones que confirmes.

La proyección a Elasticsearch tarda un par de segundos tras correr el script.

## Nota sobre el reindex

`employee_balances` faltaba en el reindex de arranque de search-service: tenía
documento e índice, pero ninguna entidad lo alimentaba. Ya está incluido, así que
al levantar search-service con `saas.search.reindex.enabled=true` también
reconstruye los saldos desde finance. Es la red de seguridad si la proyección
asíncrona fallara.
