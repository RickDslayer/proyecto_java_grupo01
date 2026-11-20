# Conexión JDBC local

Cada integrante ejecuta su propio MySQL en localhost:3306 y mantiene su propia
base de datos. No se necesita un servidor compartido ni publicar un puerto.
Usar Java 17+, Maven y MySQL 8.0.16+.

## Preparación de cada equipo

Desde la raíz del proyecto, abrir el cliente MySQL como administrador:

```sh
mysql -u root -p
```

En el cliente (compatible también con Windows):

```sql
SOURCE database/schema.sql;
SOURCE database/data.sql;
```

`schema.sql` crea `carmotors_test` y, al final, el usuario `carmotors_admin`
para `localhost` con sus permisos. Debe ejecutarse con un usuario administrador.
**Volver a ejecutar schema.sql borra y recrea esa base**, por lo que debe usarse
solo al preparar/reinicializar datos locales. No lo ejecuta ConexionFactory.
`data.sql` carga los catálogos y datos de práctica después de crear el esquema.

## Configuración en código

`ConexionFactory.java` contiene las constantes `URL`, `USUARIO` y `CLAVE`.
Se usa directamente el usuario y la contraseña de `database/schema.sql`.
La base es `carmotors_test`, exactamente como `CREATE DATABASE` y `USE`
en `database/schema.sql`; el servidor es localhost:3306 en cada equipo.
No se usan variables de entorno, propiedades JVM ni archivos properties.
Si se cambia la contraseña del script, se debe cambiar también `CLAVE` y
recompilar. Las opciones sin TLS de la URL son para desarrollo local.

## Uso desde la capa de servicio

```java
try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
    // Pasar esta misma conexión a los DAO de la operación.
}
```

`ConexionFactory` es Singleton, pero cada llamada a `crearConexion()` devuelve
una conexión nueva. El servicio cierra la conexión. Para operaciones múltiples,
el servicio usa `setAutoCommit(false)`, `commit()` y `rollback()` sobre esa misma
conexión; el DAO no confirma ni cierra conexiones recibidas.

Connector/J se registra automáticamente por JDBC; no hace falta Class.forName.

## Compilar y comprobar

```sh
mvn test
mvn package
```

Las pruebas automáticas usan un driver controlado: verifican el Singleton,
conexiones independientes, cierre por el llamador y propagación de errores,
sin necesitar MySQL ni ejecutar scripts destructivos.

Para verificar el servidor real con las credenciales locales:

```sh
mysql -h localhost -P 3306 -u carmotors_admin -p carmotors_test
```

Dentro del cliente: `SELECT DATABASE(), CURRENT_USER();`.
Esto verifica MySQL, pero la comprobación JDBC real debe ejecutarse también con
`crearConexion()` cuando el servidor esté disponible. Esta tarea no añade Main
ni menús de negocio.
