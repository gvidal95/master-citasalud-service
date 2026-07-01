# Quality gate prueba de bloqueo 

## Prueba de bloqueo

![alt text](./img/image1html.png)

Para realizar la prueba del Gate, se va a proceder a dejar FR-008 sin prueba. Esta prueba verifica que no se pueda reservar una cita si el paciente ya tiene una cita confirmada con el mismo médico el mismo día.

![alt text](./img/test1.png)

Se procede a comentar la prueba.

![alt text](./img/test2.png)

Ejecutar el comando /quality:verify

![alt text](./img/command1.png)

El Gate genera un bloqueo, FR-008 no cumple.

![alt text](./img/bloqueo.png)

El agente muestra una explicación detallada del problema.

![alt text](./img/bloqueexp.png)

El agente muestra recomendaciones para solucionar el bloqueo.

![alt text](./img/bloquerec.png)

## Prueba luego de corregir el archivo de pruebas

Se procede nuevamente a activar la prueba.

![alt text](./img/testredo.png)

Ejecutar el comando /quality:verify

![alt text](./img/command1.png)

En los resultados el criterio el agente verifica que la prueba de FR-008 cumple.

![alt text](./img/finalresult.png)