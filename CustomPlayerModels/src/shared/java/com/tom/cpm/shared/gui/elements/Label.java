package com.tom.cpm.shared.gui.elements;

import com.tom.cpm.shared.gui.IGui;

public class Label extends GuiElement {
	private String text;
	private int color;
	private Tooltip tooltip;
	public Label(IGui gui, String text) {
		super(gui);
		this.text = text;
		this.color = gui.getColors().label_text_color;
	}

	@Override
	public void draw(int mouseX, int mouseY, float partialTicks) {
		gui.drawText(bounds.x, bounds.y, text, color);

		if(bounds.isInBounds(mouseX, mouseY)) {
			if(tooltip != null)tooltip.set();
		}
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTooltip(Tooltip tooltip) {
		this.tooltip = tooltip;
	}
}
