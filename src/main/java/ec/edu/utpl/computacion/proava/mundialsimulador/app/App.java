package ec.edu.utpl.computacion.proava.mundialsimulador.app;

import ec.edu.utpl.computacion.proava.mundialsimulador.dao.ConfederacionDAO;
import ec.edu.utpl.computacion.proava.mundialsimulador.dao.EquipoDAO;
import ec.edu.utpl.computacion.proava.mundialsimulador.dao.GrupoDAO;
import ec.edu.utpl.computacion.proava.mundialsimulador.modelo.*;
import ec.edu.utpl.computacion.proava.mundialsimulador.simulador.SimuladorPartido;
import ec.edu.utpl.computacion.proava.mundialsimulador.simulador.SimuladorPartidoFifa;
import java.util.List;

/**
 * Hello world!
 */
public class App {

    static void main(String[] args) {
        System.out.println("=== Simulador Mundial 2026 - UTPL");
        System.out.println("Programación Avanzada - Abril-Agosto 2026");
        System.out.println();

        ConfederacionDAO confDAO = new ConfederacionDAO();
        EquipoDAO equipoDAO = new EquipoDAO();
        GrupoDAO grupoDAO = new GrupoDAO();

        mostrarResumen(equipoDAO, confDAO);
        mostrarTop10(equipoDAO);
        mostrarGrupos(grupoDAO);
        mostrarAnfitriones(equipoDAO);
        probarBusquedaPorCodigo(equipoDAO);

        probarSimuladorConGrupoE(grupoDAO);
    }

    private static void mostrarResumen(
        EquipoDAO equipoDAO,
        ConfederacionDAO confDAO
    ) {
        System.out.println("Equipos clasificados por confederación:");
        System.out.println("---------------------------------------");

        List<Confederacion> confederaciones = confDAO.listarTodos();
        long total = 0;
        for (Confederacion c : confederaciones) {
            List<Equipo> equipos = equipoDAO.listarPorConfederacion(c.getId());
            System.out.printf(
                "  %-10s %2d equipos%n",
                c.getCodigo(),
                equipos.size()
            );
            total += equipos.size();
        }

        System.out.println("---------------------------------------");
        System.out.printf("  TOTAL      %2d equipos%n%n", total);
    }

    private static void mostrarTop10(EquipoDAO equipoDAO) {
        System.out.println("Top 10 equipos por ranking FIFA:");
        System.out.println("--------------------------------");
        List<Equipo> equipos = equipoDAO.listarTodos();
        equipos
            .stream()
            .limit(10)
            .forEach(e -> System.out.println("  " + e));
        System.out.println();
    }

    private static void mostrarAnfitriones(EquipoDAO equipoDAO) {
        System.out.println("Países anfitriones:");
        System.out.println("-------------------");
        equipoDAO
            .listarTodos()
            .stream()
            .filter(Equipo::isAnfitrion)
            .forEach(e -> System.out.println("  " + e));
        System.out.println();
    }

    private static void probarBusquedaPorCodigo(EquipoDAO equipoDAO) {
        System.out.println("Búsqueda por código ISO:");
        System.out.println("------------------------");
        equipoDAO
            .buscarPorCodigoIso("ECU")
            .ifPresentOrElse(
                e -> System.out.println("  Encontrado: " + e),
                () -> System.out.println("  ECU no encontrado")
            );
        equipoDAO
            .buscarPorCodigoIso("XXX")
            .ifPresentOrElse(
                e -> System.out.println("  Encontrado: " + e),
                () -> System.out.println("  XXX no existe (correcto)")
            );
    }

    private static void mostrarGrupos(GrupoDAO grupoDAO) {
        System.out.println("Grupos del Mundial 2026 (sorteo oficial):");
        System.out.println("=========================================");

        List<Grupo> grupos = grupoDAO.listarTodos();

        for (Grupo g : grupos) {
            // Promedio de puntos FIFA del grupo: indicador de "dificultad"
            double promedioPuntos = g
                .getEquipos()
                .stream()
                .mapToDouble(Equipo::getPuntosFifa)
                .average()
                .orElse(0.0);

            System.out.printf(
                "%nGrupo %s  (promedio: %.1f pts FIFA)%n",
                g.getNombre(),
                promedioPuntos
            );
            System.out.println("---------");

            for (Equipo e : g.getEquipos()) {
                System.out.printf("  %s%n", e);
            }
        }
        System.out.println();
    }

    private static void probarSimuladorConGrupoE(GrupoDAO grupoDAO) {
        System.out.println();
        System.out.println("Prueba del simulador: Grupo E del Mundial");
        System.out.println("==========================================");

        Grupo grupoE = grupoDAO
            .buscarPorNombre("E")
            .orElseThrow(() -> new RuntimeException("Grupo E no encontrado"));

        SimuladorPartido simulador = new SimuladorPartidoFifa();

        // Los 6 enfrentamientos de un grupo de 4 (combinaciones C(4,2))
        List<Equipo> equipos = grupoE.getEquipos();
        System.out.println("\nUna simulación de cada enfrentamiento:");
        for (int i = 0; i < equipos.size(); i++) {
            for (int j = i + 1; j < equipos.size(); j++) {
                Equipo local = equipos.get(i);
                Equipo visitante = equipos.get(j);
                ResultadoPartido r = simulador.simular(local, visitante, true);
                System.out.println("  " + r);
            }
        }

        // Análisis de 1000 simulaciones de Francia vs Iraq
        System.out.println("\n1000 simulaciones de Ecuador vs Alemania:");
        Equipo ecuador = equipos.get(0); // posición 1: Francia
        Equipo alemania = equipos.get(2); // posición 3: Iraq

        int ganaEcu = 0,
            empates = 0,
            ganaAlm = 0;
        int totalGolesEcu = 0,
            totalGolesAlm = 0;

        for (int n = 0; n < 1000; n++) {
            ResultadoPartido r = simulador.simular(ecuador, alemania, true);
            totalGolesEcu += r.getGolesLocal();
            totalGolesAlm += r.getGolesVisitante();
            if (r.esEmpate()) empates++;
            else if (r.getGanador().equals(ecuador)) ganaEcu++;
            else ganaAlm++;
        }

        System.out.printf(
            "  Ecuador gana: %3d%% (%d veces)%n",
            ganaEcu / 10,
            ganaEcu
        );
        System.out.printf(
            "  Empate:       %3d%% (%d veces)%n",
            empates / 10,
            empates
        );
        System.out.printf(
            "  Alemania gana:    %3d%% (%d veces)%n",
            ganaAlm / 10,
            ganaAlm
        );
        System.out.printf(
            "  Goles promedio: Ecuador %.2f - Alemania %.2f%n",
            totalGolesEcu / 1000.0,
            totalGolesAlm / 1000.0
        );
    }
}
