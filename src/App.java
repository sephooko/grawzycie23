import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


class Komorka extends Thread {
    private int value;
    private final int row;
    private final int column;
    private int kolejnaGeneracja = -1;
    private final Mapa Mapa;
    private ThreadMonitor monitor = null;
    
    public Komorka(final Mapa Mapa, final int row, final int column, final int value) {
        super("Wątek");
        this.Mapa = Mapa;
        this.row = row;
        this.column = column;
        this.value = value;
    }
    
    public final void setMonitor(ThreadMonitor sprawdzWatek) {
        this.monitor = sprawdzWatek;
    }
    
    public final void run() {
        if (monitor == null) {
            throw new GameLogicException("Wątek musi zostac sprawdzony");
        }
        
        while (!isInterrupted()) {
            grajGre();
        }
        
        System.out.println(getName() + " [" + row + "," + column + "] "+ "zamknięty.");
        
    }

    private synchronized void grajGre() {
        calcKolejGen();
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        setKolejGen();
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public final synchronized void Kontynuuj() {
        notify();
    }
    
    public final synchronized void Zatrzymaj() {
        notify();
        interrupt();
    }

    private void calcKolejGen() {
        kolejnaGeneracja = getKolejGen();
        monitor.ZakKalkulacji();
    }

    public final int getKolejGen() {
        if (value == 1) {
            return getKolejGen1();
        } else {
            
            return getKolejGen0();
        }
    }

    private int getKolejGen1() {

        int iloscSasiadow = IleSasiadow();
        if (iloscSasiadow < 2 || iloscSasiadow > 3) {
            return 0;
        }
        
        return 1;
    }

    private int getKolejGen0() {

        int iloscSasiadow = IleSasiadow();
        if (iloscSasiadow == 3) {
            return 1;
        }
        
        return 0;
    }
    

    private int IleSasiadow() {
        int ilosc = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = column - 1; j <= column + 1; j++) {
                Komorka sasiad = Mapa.OdczytajKomorke(i, j);
                
                if (sasiad == this) {
                    continue; // Do not count this.
                }
                
                if (sasiad != null) {
                    if (sasiad.getValue() == 1) {
                        ilosc++;
                    }
                }
            }
        }
        
        return ilosc;
    }

    private void setKolejGen() {
        if (kolejnaGeneracja != 0 && kolejnaGeneracja != 1) {
            throw new GameLogicException("Wartość przyjeta inną niz 0 lub 1.");
        }
        
        value = kolejnaGeneracja;
        kolejnaGeneracja = -1;
        monitor.setKolejW();
    }

    public final int getValue() {
        return value;
    }

    public final int getRow() {
        return row;
    }
    
    public final int getColumn() {
        return column;
    }
}

class Mapa {
    
    private Komorka[][] Mapa;
    private final int columns;
    private final int rows;
    
    public Mapa(final int rows, final int columns) {
        this.columns = columns;
        this.rows = rows;
        StworzKomorki();
    }

    public void WczytajWartosci(List<String> stringRows) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                char character = stringRows.get(i).charAt(j);
                int value = Integer.parseInt(String.valueOf(character));
                if (value != 0 && value != 1) {
                    throw new GameLogicException(
                            "Tablica musi składać sie z 0 lub 1 a nie " + " " + value + ".");
                }
                
                Mapa[i][j] = new Komorka(this, i + 1, j + 1, value);
            }
        }
    }
    
    public void StworzKomorki() {
        Mapa = new Komorka[rows][columns];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Mapa[i][j] = new Komorka(this, i + 1, j + 1, 0);
            }
        }
    }

    public final void WczytajMape() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                System.out.print(Mapa[i][j].getValue());
            }
            
            System.out.print("\n");
        }
    }

    public final Komorka OdczytajKomorke(final int row, final int column) {
        if (row > rows || row < 1) {
            return null;
        }
        
        if (column > columns || column < 1) {
            return null;
        }
        
        Komorka Komorka = Mapa[row - 1][column - 1];
        
        if (Komorka.getRow() != row || Komorka.getColumn() != column) {
            throw new RuntimeException("Komorka nie była tam gdzie powinna.");
        }
        
        return Komorka;
    }
    
    public final List<Komorka> getKomorki() {
        ArrayList<Komorka> Komorki = new ArrayList<>();
        
        for (int i = 0; i < rows; i++) {
            Komorki.addAll(Arrays.asList(Mapa[i]).subList(0, columns));
        }
        
        return Komorki;
    }

}

class ThreadMonitor extends Thread {
    
    private final Mapa newSesjaMapy;
    private ArrayList<Komorka> Komorki = null;
    private int IloscGen = 10;
    private int tmp1 = 0;
    private int tmp2 = 0;
    

    
    public ThreadMonitor(final Mapa Mapa) {
        this.newSesjaMapy = Mapa;
    }
    
    public final void setIloscGen(final int IloscGen) {
        this.IloscGen = IloscGen;
    }
    
    public final void Start() {
        setMonitor();
        startM();
        liczGeneracje();
    }
    


    private void setMonitor() {
        Komorki = (ArrayList<Komorka>) newSesjaMapy.getKomorki();
        for (Komorka Komorka : Komorki) {
            Komorka.setMonitor(this);
        }
    }
    
    private void startM() {
        for (Komorka Komorka : Komorki) {
            Komorka.start();
        }
    }
    
    private synchronized void liczGeneracje() {

        for (int i = 1; i <= IloscGen; i++) {
            Pauza1();           
            wznowWatek();
            Pauza2();
            
            wyswietlGen(i);

            

            if (i < IloscGen) {
                wznowWatek(); 
            }
        }
        
        ZatrzymajWatek();
        wznowWatek(); 

    }
   
    private void Pauza1() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void Pauza2() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void wznowWatek() {
        for (Komorka Komorka : Komorki) {
            Komorka.Kontynuuj();
        }
    }

    private void wyswietlGen(int nrGen) {
        System.out.println("---------- " + "Generacja" + " " + nrGen + " ----------");
        newSesjaMapy.WczytajMape();
    }
      
    private void ZatrzymajWatek() {
        for (Komorka Komorka : Komorki) {
            Komorka.Zatrzymaj();
        }
    }

    public final synchronized void ZakKalkulacji() {
        tmp1++;
        
        if (tmp1 == Komorki.size()) {
            tmp1 = 0;
            notify();
        }
    }
    
    public final synchronized void setKolejW() {
        tmp2++;
        
        if (tmp2 == Komorki.size()) {
            tmp2 = 0;
            notify();
        }
    }

}

class Sesja {
    
    private Mapa Mapa = null;
    private ThreadMonitor sprawdzWatek = null;
    public void StartSym(final String inputFile, final int IloscGen) throws IOException {
        StworzMape(inputFile);
        setMonitor(IloscGen);
        Start();
    }
    //stworzenie listy stringów z Mapay txt
    private void StworzMape(final String inputFile) throws IOException {
        List<String> rows = Plik(inputFile);
        Mapa = new Mapa(rows.size(), rows.get(0).length());
        Mapa.WczytajWartosci(rows);
        
        System.out.println("Zaimportowano Mapaę" + ":");
        Mapa.WczytajMape();
    }

    private List<String> Plik(final String inputFile) throws IOException {
        ArrayList<String> rows = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            String line = bufferedReader.readLine();

            while (line != null) {
                rows.add(line);
                line = bufferedReader.readLine();
            }
        }
        
        return rows;
    }

    private void setMonitor(int IloscGen) {
        sprawdzWatek = new ThreadMonitor(Mapa);
        sprawdzWatek.setIloscGen(IloscGen);
    }

    private void Start() {
        System.out.println("Uruchamianie symulacji...");
        sprawdzWatek.Start();
    }
    
}

 class GameLogicException extends RuntimeException {
    public GameLogicException(final String message) {
        super(message);
    }
}

 class GraWZycie {
     public static void run(final String inputFile, final int IloscGen) throws IOException {
        System.out.println("Uruchamianie symulacji...");
        Sesja nowaSesja = new Sesja();
        nowaSesja.StartSym(inputFile, IloscGen);
        System.out.println("Zatrzymywanie symulacji.");
    }    
}

public class App {
    public static void main(String[] args) {
        Scanner reader = new Scanner(System.in);
        System.out.println("Ile generacji ma zasymulować program?");
        int n = reader.nextInt();
        try {
                GraWZycie.run("Mapa.txt", n);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
