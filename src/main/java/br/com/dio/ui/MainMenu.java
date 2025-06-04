package br.com.dio.ui;

import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardColumnKindEnum;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.BoardService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.*;

public class MainMenu {
    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    public void execute() throws SQLException {
        while (true) {
            System.out.println("Bem vindo ao gerenciador de boards, escolha uma opção");

            System.out.println("1 - Criar um novo board");
            System.out.println("2 - Selecionar um board existente");
            System.out.println("3 - Excluir um board");
            System.out.println("4 - Sair");

            int option = -1;

            try{
                option = Integer.parseInt(
                        scanner.nextLine().trim()
                );

            } catch (NumberFormatException e) {
                System.out.println("Opção inválida, informe uma opção do menu.") ;
                continue;
            }

            switch (option) {
                case 1 -> createBoard();
                case 2 -> selectBoard();
                case 3 -> deleteBoard();
                case 4 -> System.exit(0);
                default -> System.out.println("Opção inválida, informe uma opção do menu.\n");
            }
        }
    }

    private void createBoard() throws SQLException{
        try {
            var entity = new BoardEntity();
            System.out.println("Informe o nome do seu board. ");
            String nomeBoard = scanner.next().trim();
            scanner.nextLine();

            if (nomeBoard.isEmpty() || nomeBoard == null || nomeBoard.equals(" ")) {
                throw new RuntimeException("Nome inválido. O valor não pode ser vazio.\n");
            }

            entity.setName(nomeBoard);

            String option;
            var additionalColumns = 0;
            // while (true) { tentar receber int
            do {
                System.out.println("Seu board terá colunas além das 3 padrões? Se sim informe quantas, senão digite '0'");

                option = scanner.nextLine().trim();

                if (option.isEmpty() ) {
                    System.out.println("Opção inválida. Informe um número.\n");
                    continue;
                }
                additionalColumns = Integer.parseInt(option);

                List<BoardColumnEntity> columns = new ArrayList<>();

                System.out.println("Informe o nome da coluna inicial do board. ");
                var initialColumnName = scanner.next();
                var initialColumn = createColumn(initialColumnName, INITIAL, 0);
                columns.add(initialColumn);

                for(int i = 0; i < additionalColumns; i++) {
                    System.out.println("Informe o nome da coluna de tarefa pendente do board. ");
                    var pendingColumnName = scanner.next();
                    var pendingColumn = createColumn(pendingColumnName, PENDING, i + 1);
                    columns.add(pendingColumn);
                }

                System.out.println("Informe o nome da coluna final. ");
                var finalColumnName = scanner.next();
                var finalColumn = createColumn(finalColumnName, FINAL, ++additionalColumns );
                columns.add(finalColumn);

                System.out.println("Informe o nome da coluna de cancelamento do baord. ");
                var cancelColumnName = scanner.next();
                var cancelColumn = createColumn(cancelColumnName, CANCEL, ++additionalColumns );
                columns.add(cancelColumn);

                entity.setBoardColumns(columns);
                try(var connection = getConnection()) {
                    var service = new BoardService(connection);
                    service.insert(entity);
                }
            } while (option.isEmpty());
            System.out.println("Boarde criado com sucesso.");
        } catch (Exception e) {
            System.out.printf("Não foi possível criar board. " + e.getMessage() + "\n") ;
        }
    }

    private void selectBoard() throws SQLException{
        System.out.println("Informe o id do board que deseja selecionar. ");
        var id = scanner.nextLong();
        try(var connection = getConnection()) {
            var service = new BoardQueryService(connection);
            var optional = service.findById(id);
            optional.ifPresentOrElse(
                    b -> new BoardMenu(b).excute(),
                    () -> System.out.printf("Não foi encontrado board com o id %s|n", id)
            );
        }
    }

    private void deleteBoard() throws SQLException{
        System.out.println("Informe o id do boarde que deseja excluir. ");
        var id = scanner.nextLong();
        try(var connection = getConnection()) {
            var service = new BoardService(connection);
            if (service.delete(id)) {
                System.out.printf("O board %s foi excluído com sucesso\n", id);
            } else {
                System.out.printf("Não foi encontrado um board com id %s\n", id);
            }
        }
    }

    private BoardColumnEntity createColumn(final String name, final BoardColumnKindEnum kind, final int order) {
        var boardColum = new BoardColumnEntity();
        boardColum.setName(name);
        boardColum.setKind(kind);
        boardColum.setOrder(order);
        return boardColum;
    }
}
