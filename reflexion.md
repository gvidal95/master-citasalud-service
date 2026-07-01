# Reflexión

- ¿Qué cambió en tu forma de "dar por terminado" el código cuando el veredicto lo decidió un gate determinista en vez de tu propio criterio?

En mi experiencia, mi propio criterio no siempre aseguraba que todo estuviera terminado. Existen muchas verificaciones y validaciones que una persona puede pasar por alto
sin darse cuenta, muchas veces dando como resultado que el producto no cumpla con los estándares definidos. Este gate es una herramienta de apoyo que asegura que las personas
del proyecto no omitan ningún aspecto relacionado con la calidad del producto.

- ¿Qué pilar te costó más dejar en verde —pruebas, seguridad o criterios—, y por qué?

El pilar de criterios fue el más difícil de cumplir. Aunque se pudo verificar que los requisitos funcionales estuvieran cubiertos por pruebas, 
el agente indica en los criterios que fallaron que hay escenarios concretos o alternativos que necesitaban ser verificados con pruebas y que no fueron tomados en cuenta.

- ¿Para qué te serviría un gate de Definition of Done (y el escaneo automático de seguridad vía MCP) en tu equipo real?

Esto ayudaría a reducir el número de errores que se suelen dar en producción. Al no existir un bloqueo estricto que evite estas malas prácticas, muchas veces mi equipo de trabajo sube cambios a producción
sin respaldarse de suficientes pruebas. Esto da como resultado que errores se detecten muy tarde y sea más costoso hacer correcciones.