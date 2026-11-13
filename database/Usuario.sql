-- =============================================================================
-- CarMotors - database/usuario.sql
-- Crea el usuario de MySQL que usará ConexionFactory.java para conectarse.
-- =============================================================================
--
-- CUÁNDO Y COMO EJECUTARLO
--   Se ejecuta UNA vez, con el usuario root, ANTES de schema.sql:
--     mysql -u root -p < database/usuario.sql
--     mysql -u root -p < database/schema.sql
--     mysql -u root -p < database/data.sql
--   Se puede repetir sin error: si el usuario ya existe, solo le cambia la clave.
--
-- ANTES DE USARLO
--   1. Cambia la clave 'CambiarEstaClave123!' por una propia.
--   2. Esa MISMA clave va en el archivo de configuración que lee ConexionFactory.
--   3. NO subas la clave real a GitHub (usa .gitignore para el archivo con la
--      clave y sube solo una copia de ejemplo con la clave en blanco).
--
-- DOS OPCIONES DE PERMISOS (usa solo UNA)
--   OPCION A (recomendada): administrador de las bases de CarMotors.
--     Puede crear, borrar y modificar todo dentro de carmotors_test y carmotors
--     (tablas, datos, ejecutar schema.sql y data.sql), pero NO ve ni toca otras
--     bases del computador ni puede crear otros usuarios.
--   OPCION B: superadministrador de todo el servidor MySQL (igual que root).
--     Solo si el docente lo exige. Si la clave se filtra, quien la tenga
--     controla todo MySQL, no solo este proyecto.
-- =============================================================================

-- Crear el usuario (solo conecta desde este mismo computador)
CREATE USER IF NOT EXISTS 'carmotors_admin'@'localhost'
  IDENTIFIED BY 'CambiarEstaClave123!';

-- Si ya existía, dejar la clave actualizada
ALTER USER 'carmotors_admin'@'localhost'
  IDENTIFIED BY 'CambiarEstaClave123!';

-- -----------------------------------------------------------------------------
-- OPCION A (activa): administrador solo de las bases del proyecto
-- carmotors_test = base de pruebas de schema.sql / carmotors = base real futura
-- -----------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON carmotors_test.* TO 'carmotors_admin'@'localhost';
GRANT ALL PRIVILEGES ON carmotors.*      TO 'carmotors_admin'@'localhost';

-- -----------------------------------------------------------------------------
-- OPCION B (desactivada): superadministrador. Para usarla, quita los dos
-- guiones de las dos líneas de abajo y borra o comenta las dos líneas de la
-- OPCION A.
-- -----------------------------------------------------------------------------
-- GRANT ALL PRIVILEGES ON *.* TO 'carmotors_admin'@'localhost' WITH GRANT OPTION;

FLUSH PRIVILEGES;

-- Comprobar lo que quedó asignado
SHOW GRANTS FOR 'carmotors_admin'@'localhost';