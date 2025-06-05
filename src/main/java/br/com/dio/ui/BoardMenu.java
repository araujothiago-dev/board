package br.com.dio.ui;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.persistence.entity.CardEntity;
import br.com.dio.service.BoardColumnQueryService;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.CardQueryService;
import br.com.dio.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    private final BoardEntity entity;

    public void excute(Optional<BoardEntity> optional) {
        try{
            System.out.println("Bem vindo ao gerenciador de boards, escolha uma opção");
            var option = -1;

            while (option != 9) {
                System.out.println("1 - Criar um card");
                System.out.println("2 - Mover um card");
                System.out.println("3 - Bloquear um card");
                System.out.println("4 - Desbloquear um card");
                System.out.println("5 - Cancelar um card");
                System.out.println("6 - Ver board");
                System.out.println("7 - Ver coluna com cards");
                System.out.println("8 - Ver card");
                System.out.println("9 - Voltar para o menu anterior um card");
                System.out.println("10 - Sair");

                option = readInt();

                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> System.out.println("Voltando para o menu anterior");
                    case 10 -> System.exit(0);
                    default -> System.out.println("Opção inválida, informe uma opção do menu");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }

    }

    private void createCard() throws SQLException{
        var card = new CardEntity();
        System.out.println("Título do card: ");
        card.setTitle(readString());
        System.out.println("Descrição do card: ");
        card.setDescription(readString());
        card.setBoardColumn(entity.getInitialColumn());
        try (var connection = getConnection()){
            new CardService(connection).create(card);
        }
    }

    private void moveCardToNextColumn() throws SQLException{
        System.out.println("Informe o id do card que deseja mover para a próxima coluna. ");
        var cardId = readLong();

        var boardColumnInfo = entity.getBoardColumns()
                .stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()) {
            new CardService(connection).moveToNextColumn(cardId, boardColumnInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void blockCard() throws SQLException{
        System.out.println("Informe o id do card que deseja bloquear. ");
        var cardId = readLong();

        System.out.println("Informe o motivo do bloqueio do card. ");
        var reason = readString();
        var boardColumnInfo = entity.getBoardColumns()
                .stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()) {
            new CardService(connection).block(cardId, reason, boardColumnInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void unblockCard() throws SQLException{
        System.out.println("Informe o id do card que deseja desbloquear. ");
        var cardId = readLong();

        System.out.println("Informe o motivo do desbloqueio do card. ");
        var reason = readString();
        try (var connection = getConnection()){
            new CardService(connection).unblock(cardId, reason);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cancelCard() throws SQLException{
        System.out.println("Informe o id do card que deseja cancelar. ");
        var cardId = readLong();

        var cancelColumn = entity.getCancelColumn();
        var boardColumnInfo = entity.getBoardColumns()
                .stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try(var connection = getConnection()) {
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showBoard() throws SQLException{
        try(var connection = getConnection()) {
            var optional = new BoardQueryService(connection).showBoardDetails(entity.getId());
            optional.ifPresent(
                    b -> {
                        System.out.printf("Board [%s,%s]\n", b.id(), b.name());
                        b.columns().forEach(
                                c -> System.out.printf("Column [%s] tipo: [%s] tem %s cards\n", c.name(), c.kind(), c.cardsAmount())
                        );
                    }
            );
        }
    }

    private void showColumn() throws SQLException{
        var columnsIds = entity.getBoardColumns()
                .stream()
                .map(BoardColumnEntity::getId)
                .toList();
        var selectedColumnId = -1L;
        while (!columnsIds.contains(selectedColumnId)) {
            System.out.printf("Escolha uma coluna do board %s pelo id\n", entity.getName());
            entity.getBoardColumns()
                    .forEach(
                            c -> System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind())
                    );
            selectedColumnId = readLong();
        }
        try(var connection = getConnection()) {
            var column = new BoardColumnQueryService(connection).findById(selectedColumnId);
            column.ifPresent(
                    co -> {
                        System.out.printf("Coluna %s tipo %s\n", co.getName(), co.getKind());
                        co.getCards()
                                .forEach(
                                        ca -> System.out.printf("Card %s - %s\nDescrição: %s\n", ca.getId(), ca.getTitle(), ca.getDescription())
                                );
                    }
            );
        }
    }

    private void showCard() throws SQLException{
        System.out.println("Informe o id do card que deseja visualizar. ");
        var cardId = readLong();
        try(var connection = getConnection()) {
            new CardQueryService(connection).findById(cardId)
                    .ifPresentOrElse(
                            c -> {
                                System.out.printf("Card %s - %s.\n", c.id(), c.title());
                                System.out.printf("Descrição: %s\n", c.description());
                                System.out.println(c.blocked() ?
                                        "Está bloqueado, Motivo: " + c.blockReason() :
                                        "Não está bloquado"
                                        );
                                System.out.printf("Já foi bloqueado %s veze(s)\n", c.blocksAmount());
                                System.out.printf("Está no momento na coluna %s - %s\n", c.columnId(), c.columnName());
                            },
                            () -> System.out.printf("Não existe um card com o id %s\n", cardId)
                    );
        }
    }

    private Long readLong() {
        while (true) {
            String option = scanner.nextLine().trim();
            try {
                return Long.parseLong(option);
            } catch (NumberFormatException e) {
                System.out.print("Opção inválida. Informe um número inteiro:\n");
            }
        }
    }

    private int readInt() {
        while (true) {
            String option = scanner.nextLine().trim();
            try {
                return Integer.parseInt(option);
            } catch (NumberFormatException e) {
                System.out.print("Opção inválida. Informe um número inteiro:\n");
            }
        }
    }

    private String readString() {
        while (true) {
            String option = scanner.nextLine().trim();
            if (!option.isEmpty()) {
                return option;
            }
            System.out.print("Esse campo não pode ser vazio:\n");
        }
    }

}
