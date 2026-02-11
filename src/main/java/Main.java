import models.Evenement;
import models.TypeEvenement;
import services.EvenementService;

import java.util.Date;

public class Main {
    public static void main(String[] args) {

        EvenementService service = new EvenementService();

        Evenement e1 = new Evenement(
                0,
                "html",
                "projet",
                new Date(),
                new Date(),
                "megrine",
                1,
                true,
                TypeEvenement.REUNION
        );

        service.add(e1);

        System.out.println("=== LISTE ===");
        service.getAll().forEach(System.out::println);

        e1.setTitre("Conf Java UPDATE");
        service.update(e1);

        service.delete(1);
    }
}
