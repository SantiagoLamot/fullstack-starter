import { useEffect, useState } from 'react';
import { Animated, type DimensionValue, StyleSheet } from 'react-native';

import { colors, radii } from '@/theme';

interface SkeletonProps {
  width?: DimensionValue;
  height: number;
}

const PULSE_MS = 900;

/** Placeholder de carga: pulso suave de opacidad (ease, sin rebotes). */
export function Skeleton({ width = '100%', height }: SkeletonProps) {
  const [opacity] = useState(() => new Animated.Value(0.5));

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, { toValue: 1, duration: PULSE_MS, useNativeDriver: true }),
        Animated.timing(opacity, { toValue: 0.5, duration: PULSE_MS, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [opacity]);

  return <Animated.View style={[styles.base, { width, height, opacity }]} />;
}

const styles = StyleSheet.create({
  base: { backgroundColor: colors.border, borderRadius: radii.sm },
});
