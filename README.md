# busTrips

Prikaže avtobuse, ki v naslednjih dveh urah pridejo na izbrano postajališče.

## Zagon

```bash
mvn package
java -Dgtfs.dir=/pot/do/gtfs -jar target/bustrips-1.0-SNAPSHOT.jar <station_id> <num_buses_per_line> <relative|absolute> [reference_time]
```

| Argument | Pomen |
| --- | --- |
| `station_id` | `stop_id` ali `stop_code` iz `stops.txt` |
| `num_buses_per_line` | največ toliko prihodov na linijo |
| `relative` \| `absolute` | `10min` ali `12:10` |
| `reference_time` | neobvezen, npr. `2020-03-02T07:00` |

Mapo s feedom podaš z `-Dgtfs.dir` ali `$GTFS_DIR`, sicer aplikacija bere delovno mapo. Naloga predpisuje tri pozicijske argumente, zato mape nisem dodal kot četrtega. Izhodne kode: `0` uspeh, `1` nečitljiv feed, `2` napačni argumenti, `3` neznana postaja.

```
$ ... 2 2 absolute 2020-03-02T07:00      $ ... 2 2 relative 2020-03-02T07:00
AL Masjid Al-nabawi (Clock Roundabout)   AL Masjid Al-nabawi (Clock Roundabout)

101   07:00   07:04                      101   0min   4min
106   07:00   07:01                      106   0min   1min
107   07:00   07:05                      107   0min   5min
```

Aplikacija odpre `stops.txt`, `stop_times.txt`, `trips.txt`, `routes.txt` in `calendar.txt`, če obstaja. Imena datotek sem zapisal samo v `GtfsFeed`.

## Zgradba

| Razred | Naloga |
| --- | --- |
| `Main` | argumenti, izhodne kode |
| `ArrivalBoard` | poveže korake v en odgovor |
| `GtfsFeed` | branje feeda, ena metoda na datoteko |
| `CsvReader` | odpiranje datoteke, glava, BOM, prazne vrstice |
| `Csv` | polja v eni vrstici |
| `Parsers` | razčlenjevanje z berljivo napako |
| `GtfsTime` | `25:30:00` v `Duration` |
| `ServiceDayWindow` | okno v časih servisnega dne |
| `ArrivalFormat`, `BoardPrinter` | izpis |
| `Stop`, `StopArrival`, `TripInfo`, `ServiceCalendar`, `RouteArrivals`, `StopBoard` | podatkovni zapisi |

Vse datoteke berem prek `CsvReader`, ki odpre datoteko, prebere glavo in preskoči prazne vrstice. Vožnje in linije iščem z isto metodo `lookUp`, ki neha brati, ko najde zadnji ključ. Ure, datume, argumente in format razčlenim prek `Parsers`, ki ob napaki vrže `IllegalArgumentException` z enotnim sporočilom.

## Odločitve

### Kaj je `station_id`

Naloga pravi `int` s primerom `12345`. V feedu ima `stop_id` vrednosti od 2 do 13, `stop_code` pa od 2001 do 2012, zato primer ne ustreza nobenemu. `stop_id` je interni ključ, `stop_code` pa številka na tabli, ki jo vidi potnik. Sprejmem oba. Ob trku zmaga `stop_id`, cena pa je ena primerjava več na vrstico `stops.txt`.

Vrednost hranim kot niz, ker jo specifikacija tipizira tako. Feed z oznako tipa `CENTRAL_1` bi ob pretvorbi v `int` vrgel izjemo, zato sem tip iz naloge prezrl.

### Vrstni red branja

V `stop_times.txt` je čas prihoda na postajo, linije pa ni. V `trips.txt` je linija, postaje pa ni. Rabim obe.

Filtriram lahko po postaji, oknu in koledarju. Najprej izberem postajo, ker izloči največ vrstic. Feed velikega mesta ima v `stop_times.txt` okoli osem milijonov vrstic, ena postaja čez dan nekaj sto, v dvournem oknu pa nekaj deset.

Zato preberem `stop_times.txt` prvi in v `trips.txt` poiščem le preostalih petindvajset `trip_id`. V obratnem vrstnem redu bi moral v pomnilnik naložiti dvesto tisoč voženj. Poraba pomnilnika tako raste s prometnostjo postaje, z velikostjo feeda pa ne.

Po času filtriram v istem prehodu, čeprav še ne vem, ali vožnja tisti dan vozi. Obdržim kakšno vrstico preveč in jo kasneje zavržem. Če bi zavrgel preveč, bi moral osem milijonov vrstic prebrati znova.

Koledar preberem v celoti, ker ima `calendar.txt` eno vrstico na vzorec voznega reda in ostane majhen tudi pri velikih feedih.

### Kaj `Csv.fieldEquals` prihrani

Polje primerjam na mestu z `String.regionMatches` in vrstice ne razbijem. Vrstica druge postaje tako ne ustvari nobenega niza.

Vršne porabe pomnilnika s tem ne zmanjšam. Z `line.split(",")` bi bila enaka, ker zbiralnik smeti sproti pobere prejšnje vrstice. Zmanjšam število alokacij, kar pomaga pri hitrosti. Na zahtevo naloge o velikosti naloženih podatkov odgovarja filtriranje med branjem.

Metodo sem obdržal, ker se izvede osemmilijonkrat na poizvedbo in ker ima `Csv` svoje teste. Drugod uporabljam `Csv.field`. Pri izboru prvih N zberem vse prihode linije in jih nato odrežem, saj pri nekaj deset elementih omejena struktura ne bi prinesla ničesar.

### Čas

`LocalTime` sprejme največ `23:59:59`, GTFS pa zapiše `25:30:00`. `arrival_time` je odmik od polnoči servisnega dne in `Duration` hrani prav tak odmik.

Okno pokriva absolutne trenutke, zato ga za vsak servisni dan prevedem v njegove čase. V petek ob 23:30:

```
servisni dan PETEK:    arrival_time med 23:30 in 25:30
servisni dan SOBOTA:   arrival_time med 00:00 in 01:30
```

Sobotna spodnja meja bi znašala `-00:30`, zato jo postavim na nič.

`00:45` in `24:45` sta isti trenutek, a pripadata različnima servisnima dnevoma. Vsak zadetek zato hrani svoj servisni dan, ki ga preverim v `calendar.txt`.

Gledam tudi dan nazaj. V soboto ob 00:30 pride vožnja s petkovega servisnega dne s časom `24:30` čez štirideset minut.

Časovni pas vzamem iz sistema, ker naloga `agency.txt` ne navaja. V Sloveniji aplikacija nad feedom iz Medine računa po srednjeevropskem času. Znotraj enega feeda to rezultata ne spremeni, za produkcijo pa bi bral `agency_timezone`.

`ServiceDayWindow.MAX_SERVICE_TIME` sem nastavil na 32 ur, ker GTFS meje ne določa. Relativni čas zaokrožim navzdol, zato `5min` pomeni vsaj pet minut.

### Referenčni čas

Četrti argument je neobvezen, trije iz naloge ostanejo enaki. Feed velja do 15. maja 2020, zato bi aplikacija danes izpisala prazno tablo, čeprav bi delovala pravilno.

Argument rabim tudi za teste. Trenutek podam kot vhod, zato testi za okno čez polnoč in pogled nazaj ob vsakem zagonu dajo isti rezultat.

### Izpis

Za vsako linijo izpišem eno vrstico, kot na tabli na postajališču. Linije razvrstim po oznaki, da med poizvedbami ostanejo na istem mestu. Številske oznake razvrstim po vrednosti, zato je `9` pred `10`.

`GtfsFeed.arrivalsAt` vrne `List`, ker ima rezultat nekaj deset elementov in ga tako lažje testiram. S `Stream` bi moral klicatelj zapreti datoteko, s povratnim klicem pa bi obrnil nadzor.

## Testi

```bash
mvn test      # unit testi (*Test), Surefire
mvn verify    # tudi integracijski (*IT), Failsafe
```

| Testni razred | Kaj pokriva |
| --- | --- |
| `GtfsTimeTest` | `25:30:00`, enomestne ure, napačen vhod |
| `ParsersTest` | razčlenjevanje in meje števil |
| `CsvTest` | narekovaji, prazna polja, primerjava na mestu |
| `ServiceDayWindowTest` | okno čez polnoč, pogled nazaj |
| `GtfsFeedTest` | iskanje postaje, filtriranje prihodov, vožnje, linije, koledar |
| `ArrivalFormatTest` | oba formata izpisa |
| `BoardPrinterTest` | postavitev table |
| `BusTripsIT` | od argumentov do izpisa |

Unit test preverja en razred. `GtfsFeedTest` zapiše majhne datoteke v `@TempDir`, ker `GtfsFeed` bere datoteke in ga testiram na pravih datotekah namesto na nadomestku. Za integracijski test štejem test, ki poveže več delov sistema.

`BusTripsIT` požene aplikacijo od argumentov do izpisa in izhodne kode. Z njim ujamem napake v povezovanju, na primer zamenjan argument ali napačno izhodno kodo. Teče na `src/test/resources/mini-feed`, ročno napisanem feedu z dvema postajama in tremi linijami. Pravega feeda v testih ne uporabljam, ker je potekel in se lahko spremeni.

S tem testom sem med razvojem našel napako v testnih podatkih: isto postajo sem uporabil za dokaz filtriranja in za primer brez prihodov.

## Omejitve

- `calendar_dates.txt` ne berem, zato praznikov in izjem ne upoštevam. Naloga datoteke ne omenja.
- Vožnjo, katere `service_id` ni v `calendar.txt`, zavržem. Raje izpustim avtobus, ki vozi, kot da pokažem avtobus, ki ne vozi.
- Vrstice s praznim `arrival_time` preskočim in časov med točkami ne interpoliram.
- Čase računam v sistemskem časovnem pasu.
- Predpostavljam, da servisni dan ne sega čez 32 ur.
- Na dan premika ure bi `polnoč + Duration` dal napačen čas. V Rijadu ure ne premikajo.
- `parent_station` prezrem, zato poizvedba pokrije en peron.

## Kako bi bila naloga težja

- Vmesne postaje. Večina voženj v tem feedu ima v `stop_times.txt` le začetek in konec. Pri feedu z vmesnimi postanki in praznimi `arrival_time` bi moral čase interpolirati.
- `calendar_dates.txt`. Izjeme za posamezne datume povozijo tedenski vzorec, zato bi moral pravila preverjati po prednosti.
- Premik ure. Na dan s 23 ali 25 urami bi moral servisni dan računati po koledarju. Test za to lahko napišem, ker trenutek podam kot vhod.
- Cela postaja. Perone bi združil prek `parent_station` in izpisal, na katerem avtobus ustavi.
- Realni čas. GTFS Realtime doda zamude in odpovedi.
- Strežnik. Pri sto poizvedbah na sekundo bi ponovno branje datotek postalo predrago, zato bi zgradil indeks po postaji in času. Tam bi izbira med `Duration` in `int` vplivala na porabo pomnilnika.
