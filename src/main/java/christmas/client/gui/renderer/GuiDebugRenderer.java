/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.renderer;

import christmas.client.ChristmasClient;
import christmas.client.gui.utils.Cell;
import christmas.client.gui.widgets.WWidget;
import christmas.client.gui.widgets.containers.WContainer;
import christmas.client.renderer.MeshBuilder;
import christmas.client.renderer.MeshRenderer;
import christmas.client.renderer.ChristmasRenderPipelines;
import christmas.client.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public class GuiDebugRenderer {
    private static final Color CELL_COLOR = new Color(25, 225, 25);
    private static final Color WIDGET_COLOR = new Color(25, 25, 225);

    private final MeshBuilder mesh = new MeshBuilder(ChristmasRenderPipelines.WORLD_COLORED_LINES);

    public void render(WWidget widget) {
        if (widget == null) return;

        mesh.begin();
        renderWidget(widget);
        mesh.end();

        MeshRenderer.begin()
            .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
            .pipeline(ChristmasRenderPipelines.WORLD_COLORED_LINES)
            .mesh(mesh)
            .end();
    }

    public void mouseReleased(WWidget widget, MouseButtonEvent click, int i) {
        if (widget == null) return;

        ChristmasClient.LOG.info("{} {}", widget.getClass(), i);

        if (widget instanceof WContainer container) {
            for (Cell<?> cell : container.cells) {
                if (cell.widget().isOver(click.x(), click.y())) {
                    mouseReleased(cell.widget(), click, i + 1);
                }
            }
        }
    }

    private void renderWidget(WWidget widget) {
        lineBox(widget.x, widget.y, widget.width, widget.height, WIDGET_COLOR);

        if (widget instanceof WContainer container) {
            for (Cell<?> cell : container.cells) {
                lineBox(cell.x, cell.y, cell.width, cell.height, CELL_COLOR);
                renderWidget(cell.widget());
            }
        }
    }

    private void lineBox(double x, double y, double width, double height, Color color) {
        line(x, y, x + width, y, color);
        line(x + width, y, x + width, y + height, color);
        line(x, y, x, y + height, color);
        line(x, y + height, x + width, y + height, color);
    }

    private void line(double x1, double y1, double x2, double y2, Color color) {
        mesh.ensureLineCapacity();

        mesh.line(
            mesh.vec3(x1, y1, 0).color(color).next(),
            mesh.vec3(x2, y2, 0).color(color).next()
        );
    }
}
