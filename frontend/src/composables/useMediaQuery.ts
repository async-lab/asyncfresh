import { computed, onMounted, onUnmounted, ref, type Ref } from 'vue';

const MOBILE_QUERY = '(max-width: 960px)';

function getMatches(query: string) {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }

  return window.matchMedia(query).matches;
}

export function useMediaQuery(query: string): Ref<boolean> {
  const matches = ref(getMatches(query));
  let mediaQuery: MediaQueryList | undefined;

  const update = (event?: MediaQueryListEvent) => {
    matches.value = event ? event.matches : getMatches(query);
  };

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }

    mediaQuery = window.matchMedia(query);
    matches.value = mediaQuery.matches;

    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', update);
      return;
    }

    if (typeof mediaQuery.addListener === 'function') {
      mediaQuery.addListener(update);
    }
  });

  onUnmounted(() => {
    if (!mediaQuery) {
      return;
    }

    if (typeof mediaQuery.removeEventListener === 'function') {
      mediaQuery.removeEventListener('change', update);
      return;
    }

    if (typeof mediaQuery.removeListener === 'function') {
      mediaQuery.removeListener(update);
    }
  });

  return matches;
}

export function useIsMobile(query = MOBILE_QUERY) {
  return useMediaQuery(query);
}

export function useOverlayLayout(options?: {
  dialogWidth?: string;
  drawerSize?: string;
}) {
  const isMobile = useIsMobile();
  const dialogWidth = computed(() => (isMobile.value ? '96%' : options?.dialogWidth ?? '520px'));
  const drawerSize = computed(() => (isMobile.value ? '100%' : options?.drawerSize ?? '520px'));
  return { isMobile, dialogWidth, drawerSize };
}

