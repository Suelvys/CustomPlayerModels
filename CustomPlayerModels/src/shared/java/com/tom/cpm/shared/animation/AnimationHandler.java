package com.tom.cpm.shared.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.tom.cpm.shared.definition.ModelDefinition;

public class AnimationHandler {
	private final Supplier<ModelDefinition> player;

	private List<PlayingAnim> currentAnimations = new ArrayList<>();
	private List<NextAnim> nextAnims = new ArrayList<>();

	private Gesture currentGesture;

	public AnimationHandler(Supplier<ModelDefinition> player) {
		this.player = player;
	}

	public void animate(AnimationState state, long currentTime) {
		boolean needsSort = false;
		currentAnimations.removeIf(a -> nextAnims.stream().noneMatch(n -> n.animation == a.currentAnimation) && a.currentAnimation.checkAndUpdateRemove());
		for (NextAnim animation : nextAnims) {
			boolean found = false;
			for (int i = 0; i < currentAnimations.size(); i++) {
				if(currentAnimations.get(i).currentAnimation == animation.animation) {
					found = true;
					break;
				}
			}
			if(!found) {
				currentAnimations.add(new PlayingAnim(animation, currentTime,
						currentGesture != null ? (currentGesture.animation.contains(animation.animation) ? currentGesture.isLoop : true) : true));
				needsSort = true;
			}
		}
		if(needsSort)
			currentAnimations.sort(Comparator.comparingInt(e -> e.currentAnimation.getPriority()));

		player.get().resetAnimationPos();

		for (PlayingAnim a : currentAnimations) {
			if(!a.finished) {
				long currentStep = (currentTime - a.currentStart);
				a.currentAnimation.animate(a.getTime(state, currentStep), player.get());

				if(!a.loop && currentStep > a.currentAnimation.getDuration()) {
					a.finished = true;
				}
			}
		}

		nextAnims.clear();
	}

	public void addAnimations(List<? extends IAnimation> next, IPose pose) {
		next.stream().map(a -> new NextAnim(a, pose)).forEach(nextAnims::add);
	}

	public void setGesture(Gesture next) {
		currentGesture = next;
		if(next != null) {
			addAnimations(next.animation, null);
		}
	}

	public void clear() {
		currentGesture = null;
		nextAnims.clear();
		currentAnimations.clear();
	}

	private static class NextAnim {
		private IAnimation animation;
		private IPose pose;

		public NextAnim(IAnimation animation, IPose pose) {
			this.animation = animation;
			this.pose = pose;
		}
	}

	private static class PlayingAnim {
		private IAnimation currentAnimation;
		private IPose pose;
		private long currentStart;
		private boolean loop, finished;

		public PlayingAnim(NextAnim anim, long currentStart, boolean loop) {
			this.currentAnimation = anim.animation;
			currentAnimation.prepare();
			this.pose = anim.pose;
			this.currentStart = currentStart;
			this.loop = loop;
			this.finished = false;
		}

		public long getTime(AnimationState state, long time) {
			if(pose == null)return time;
			else return pose.getTime(state, time);
		}
	}
}
