FROM node:22.14.0-alpine AS build
WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN echo "VUE_CLI_BABEL_TRANSPILE_MODULES=true" > .env.local
RUN echo "ESLINT_NO_DEV_ERRORS=true" >> .env.local
RUN echo "DISABLE_ESLINT_PLUGIN=true" >> .env.local

RUN npm run build

FROM nginx:1.26.3-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]