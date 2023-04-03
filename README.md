# Путешествие ByteBuffer-ов из Publisher в Stream и обратно

## Введение. Проблема на примере задачи, которую мы решали при реализации своего проекта.
1. Представление.
2. Несколько слов про реактивное программирование и его поддержку популярными WEB фреймворками на примере [Spring](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html),
   [Vertx](https://vertx.io/docs/#reactive), [Apache CXF](https://cxf.apache.org/docs/jax-rs-project-reactor-support.html).
3. Источник данных в реактивном программировании - Publisher, несколько слов о Publisher/Subscriber.
4. Существует много хороших библиотек, которые реализованы с использованием InputStream/OutputStream.
Краткое описание нашей проблемы: парсинг и трансформация больших XML файлов.
Задача: Как из Publisher<ByteBuffer> получить InputStream, затем из OutputStream получить Publisher<ByteBuffer>?
5. Структура тестового примера: имитация получения и обработки данных, финальная проверка на корректность реализации.
```
src/test/java/org/example/BaseTest.java
```

## Решение задачи с использованием ByteArrayInputStream/ByteArrayOutputStream
1. Несколько слов про ByteArrayInputStream/ByteArrayOutputStream.
2. Описание возможной реализации на примере ByteArrayTest.
```
src/test/java/org/example/ByteArrayTest.java
```
3. Запускаем, смотрим вывод на консоль.
4. Выводы:
    - если есть задержки в получении данных, то мы блокируемся и ждём, что не эффективно.
    - используется в два раза больше памяти, чем обрабатываемые данные, что также не эффективно.
  
## Решение задачи с использованием spring-webflux, потому что spring очень популярен.
1. Несколько слов про PipedOutputStream/PipedInputStream - что это и как работает.
Работает только в разных потоках.
2. Описание возможной реализации с использованием классов библиотеки Reactor, PipedOutputStream/PipedInputStream, а так же утилитного класса org.springframework.core.io.buffer.DataBufferUtils, который, по сути и делает всю работу. Смотрим код, слайды.
```
src/test/java/org/example/SpringAndFluxTest.java
```
3. Запускаем, смотрим вывод на консоль.
4. Выводы:
    - происходит обработка поступивших данных, всех данных не ждём, обработанные данные сразу доступны для публикации.
    - проект должен использовать spring-webflux и Reactor, а так бывает не всегда.
  
## Решение задачи с использованием библиотеки ReactiveX (RxJava2)
1. Описание возможной реализации с использованием классов библиотеки RxJava2. Смотрим код, слайды.
```
src/test/java/org/example/RxJavaTest.java
```
2. Запускаем, смотрим вывод на консоль.
3. Выводы:
    - происходит обработка поступивших данных, всех данных не ждём, обработанные данные сразу доступны для публикации.
    - предложенный подход может быть так же реализован на соотв. классах библиотеки Reactor.

## Не требует доп. зависимостей, что делает его более универсальным.
Заключение. Ссылки на ресурсы.


[Заявка](https://mm.jugru.team/java-conf/channels/garus_denis_jpoint-7084)